package haslab.isolde.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.check.external.CheckingIntermediateRepresentation;
import haslab.isolde.core.check.external.HistCheckProblem;
import haslab.isolde.kodkod.Atom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;
import org.junit.jupiter.api.Test;

/**
 * Decoding a solved instance into a {@link History}. Instances are built by hand rather than solved
 * for, so that a particular session order — including ones a solver would rarely pick — can be
 * decoded on demand.
 */
public class HistoryIntermediateRepresentationTest {

  private static final DirectAbstractHistoryEncoding ENC = DirectAbstractHistoryEncoding.INSTANCE;
  private static final Atom<Integer> KEY = new Atom<>("x", 0);
  private static final Atom<Integer> V0 = new Atom<>("v", 0);
  private static final Atom<Integer> V1 = new Atom<>("v", 1);

  private static Atom<Integer> txn(int id) {
    return new Atom<>("t", id);
  }

  /**
   * An instance over transactions {@code t0..tn}, where {@code t0} is the initial transaction. Every
   * normal transaction reads the initial value of the single object and writes it back, so none is
   * empty. {@code so} is given as pairs of normal transaction ids and must already be transitive,
   * which is what the synthesis encoder guarantees; the {@code t0 -> t} edges the encoder always
   * includes are added here too.
   */
  private static Instance instance(int normalTxns, int[][] so) {
    List<Object> atoms = new ArrayList<>();
    for (int i = 0; i <= normalTxns; i++) atoms.add(txn(i));
    atoms.add(KEY);
    atoms.add(V0);
    atoms.add(V1);

    Universe u = new Universe(atoms);
    TupleFactory f = u.factory();
    Instance instance = new Instance(u);

    TupleSet txns = f.noneOf(1);
    for (int i = 0; i <= normalTxns; i++) txns.add(f.tuple(txn(i)));
    instance.add(ENC.transactions(), txns);
    instance.add(ENC.initialTransaction(), f.setOf(txn(0)));
    instance.add(ENC.keys(), f.setOf(KEY));
    instance.add(ENC.values(), f.setOf(V0, V1));

    TupleSet writes = f.noneOf(3);
    TupleSet reads = f.noneOf(3);
    writes.add(f.tuple(txn(0), KEY, V0));
    for (int i = 1; i <= normalTxns; i++) {
      reads.add(f.tuple(txn(i), KEY, V0));
      writes.add(f.tuple(txn(i), KEY, V1));
    }
    instance.add(ENC.finalWrites(), writes);
    instance.add(ENC.externalReads(), reads);

    TupleSet soTs = f.noneOf(2);
    for (int i = 1; i <= normalTxns; i++) soTs.add(f.tuple(txn(0), txn(i)));
    for (int[] edge : so) soTs.add(f.tuple(txn(edge[0]), txn(edge[1])));
    instance.add(ENC.sessionOrder(), soTs);

    return instance;
  }

  private static History decode(int normalTxns, int[][] so) {
    return new History(ENC, instance(normalTxns, so));
  }

  private static List<Integer> ids(Session session) {
    return session.transactions().stream().map(Transaction::id).toList();
  }

  @Test
  public void initialTransactionIsNotPartOfTheHistory() {
    History history = decode(2, new int[][] {{1, 2}});

    List<Integer> all =
        history.getSessions().stream().flatMap(s -> ids(s).stream()).sorted().toList();
    assertEquals(List.of(1, 2), all);
  }

  @Test
  public void sessionOrderedTransactionsFormOneSession() {
    // The regression test: t1 -> t2 -> t3, transitively closed. This used to come out as three
    // separate sessions whenever a transaction was visited before its predecessor, which made a
    // history look like it had no session order at all.
    History history = decode(3, new int[][] {{1, 2}, {2, 3}, {1, 3}});

    assertEquals(1, history.getSessions().size());
    Session session = history.getSessions().get(0);
    assertEquals(List.of(1, 2, 3), ids(session));
    assertTrue(session.isChain());
  }

  @Test
  public void chainIsDecodedIndependentlyOfTheOrderTheEdgesAreGivenIn() {
    History forwards = decode(3, new int[][] {{1, 2}, {2, 3}, {1, 3}});
    History backwards = decode(3, new int[][] {{2, 3}, {1, 3}, {1, 2}});

    assertEquals(forwards.toString(), backwards.toString());
  }

  @Test
  public void unrelatedTransactionsAreSeparateSessions() {
    History history = decode(3, new int[][] {});

    assertEquals(3, history.getSessions().size());
    assertEquals(List.of(List.of(1), List.of(2), List.of(3)), history.getSessions().stream().map(HistoryIntermediateRepresentationTest::ids).toList());
  }

  @Test
  public void twoChainsAreTwoSessions() {
    History history = decode(4, new int[][] {{1, 3}, {2, 4}});

    assertEquals(2, history.getSessions().size());
    assertEquals(List.of(1, 3), ids(history.getSessions().get(0)));
    assertEquals(List.of(2, 4), ids(history.getSessions().get(1)));
    assertTrue(history.getSessions().get(0).isChain());
    assertTrue(history.getSessions().get(1).isChain());
  }

  @Test
  public void aJoinIsOneSessionAndKeepsBothEdges() {
    // t1 -> t4 and t3 -> t4 with t1 and t3 incomparable. The synthesis encoder constrains session
    // order only to be transitive, so this really does come back from the solver; no list of
    // transactions can express it, so the session is not a chain and keeps its edges.
    History history = decode(4, new int[][] {{1, 4}, {3, 4}});

    assertEquals(2, history.getSessions().size());
    Session join = history.getSessions().get(0);
    assertEquals(List.of(1, 3, 4), ids(join));
    assertFalse(join.isChain());
    assertEquals(Set.of(List.of(0, 2), List.of(1, 2)), join.coveringEdges());
    assertEquals(List.of(2), ids(history.getSessions().get(1)));
    assertTrue(join.toString().endsWith("so: 1 -> 4, 3 -> 4"));
  }

  @Test
  public void transactionsAreListedConsistentlyWithTheSessionOrder() {
    // A fork: t1 before both t2 and t3, which are incomparable. t1 must still be listed first.
    History history = decode(3, new int[][] {{1, 2}, {1, 3}});

    Session session = history.getSessions().get(0);
    assertEquals(List.of(1, 2, 3), ids(session));
    assertFalse(session.isChain());
    assertEquals(Set.of(List.of(0, 1), List.of(0, 2)), session.coveringEdges());
  }

  @Test
  public void aChainPrintsAsASequenceAndANonChainPrintsItsEdges() {
    assertEquals(
        String.join("\n", "1: r(0,0) w(0,1) ", "|", "2: r(0,0) w(0,1) "),
        decode(2, new int[][] {{1, 2}}).toString());

    assertEquals(
        String.join(
            "\n", "1: r(0,0) w(0,1) ", "2: r(0,0) w(0,1) ", "3: r(0,0) w(0,1) ", "so: 1 -> 2, 1 -> 3"),
        decode(3, new int[][] {{1, 2}, {1, 3}}).toString());
  }

  @Test
  public void aDecodedHistoryEncodesBackToTheSameSessionOrder() {
    // Round trip: the checking encoder supplies its own initial transaction, so a decoded history
    // that still carried one would come back with two. It renumbers transactions from 1 as it walks
    // the sessions, ignoring their ids, so the session order is preserved up to that renaming:
    // sessions [1, 3, 4] and [2] become t1, t2, t3 and t4, and 1 -> 4, 3 -> 4 becomes t1 -> t3,
    // t2 -> t3.
    History history = decode(4, new int[][] {{1, 4}, {3, 4}});

    Bounds bounds =
        new HistCheckProblem(new CheckingIntermediateRepresentation(history)).encode().bounds();
    TupleFactory f = bounds.universe().factory();

    assertEquals(
        f.setOf(txn(0), txn(1), txn(2), txn(3), txn(4)), bounds.lowerBound(ENC.transactions()));
    assertEquals(f.setOf(txn(0)), bounds.lowerBound(ENC.initialTransaction()));

    TupleSet expectedSo = f.noneOf(2);
    for (int i = 1; i <= 4; i++) expectedSo.add(f.tuple(txn(0), txn(i)));
    expectedSo.add(f.tuple(txn(1), txn(3)));
    expectedSo.add(f.tuple(txn(2), txn(3)));
    assertEquals(expectedSo, bounds.lowerBound(ENC.sessionOrder()));
  }

  @Test
  public void aChainSessionBuiltByHandOrdersEveryPair() {
    Session session =
        new Session(
            Arrays.asList(
                new Transaction(1, List.of(Operation.writeOf(0, 1))),
                new Transaction(2, List.of(Operation.readOf(0, 1))),
                new Transaction(3, List.of(Operation.readOf(0, 1)))));

    assertTrue(session.isChain());
    assertEquals(
        Set.of(List.of(0, 1), List.of(0, 2), List.of(1, 2)), Set.copyOf(session.order()));
    assertEquals(Set.of(List.of(0, 1), List.of(1, 2)), session.coveringEdges());
  }
}
