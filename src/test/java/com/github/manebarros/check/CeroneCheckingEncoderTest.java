package com.github.manebarros.check;

import static com.github.manebarros.core.DirectAbstractHistoryEncoding.initialTransaction;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.instance;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.keys;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.reads;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.sessionOrder;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.sessions;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.transactions;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.txn_session;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.values;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.writes;
import static com.github.manebarros.history.Operation.readOf;
import static com.github.manebarros.history.Operation.writeOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.manebarros.cerone.CeroneDefinitions;
import com.github.manebarros.cerone.CeroneExecution;
import com.github.manebarros.core.CheckingEncoder;
import com.github.manebarros.core.DirectAbstractHistoryEncoding;
import com.github.manebarros.history.History;
import com.github.manebarros.history.Session;
import com.github.manebarros.history.Transaction;
import com.github.manebarros.kodkod.KodkodProblem;
import com.github.manebarros.kodkod.KodkodUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;
import kodkod.instance.Universe;
import org.junit.jupiter.api.Test;

/**
 * The following properties must be true for every instance when checking histories:
 *
 * <ul>
 *   <li>`ar` is a total order over transactions
 *   <li>`vis` \subseteq `ar`
 *   <li>`so` \not\subseteq `vis` is possible
 *   <li>`so` \subseteq `ar`
 *   <li>The initial txn precedes all others in `vis`
 *   <li>The initial txn precedes all others in `so`
 *   <li>The initial txn precedes all others in `ar`
 * </ul>
 */
public interface CeroneCheckingEncoderTest {
  CheckingEncoder<CeroneExecution> encoder();

  @Test
  default void arbitrationTotallyOrdersTransactions() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    KodkodProblem p =
        encoder()
            .encode(
                hist, e -> KodkodUtil.strictTotalOrder(e.ar(), e.history().transactions()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void visSubsetOfAr() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    KodkodProblem p = encoder().encode(hist, e -> e.vis().in(e.ar()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void soMightNotBeSubsetOfVis() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    KodkodProblem p = encoder().encode(hist, e -> e.history().sessionOrder().in(e.vis()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.sat());
  }

  @Test
  default void soSubsetOfAr() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    KodkodProblem p = encoder().encode(hist, e -> e.history().sessionOrder().in(e.ar()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void initialTxnIsFirstInArbitration() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    KodkodProblem p =
        encoder()
            .encode(
                hist,
                e ->
                    KodkodUtil.min(
                            e.history().initialTransaction(), e.ar(), e.history().transactions())
                        .not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void initialTxnPrecedesAllInSessionOrder() {
    Variable t = Variable.unary("t");

    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    KodkodProblem p =
        encoder()
            .encode(
                hist,
                e ->
                    t.eq(e.history().initialTransaction())
                        .or(t.in(e.history().initialTransaction().join(e.history().sessionOrder())))
                        .forAll(t.oneOf(e.history().transactions()))
                        .not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void initialTxnPrecedesAllInVis() {
    Variable t = Variable.unary("t");

    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    KodkodProblem p =
        encoder()
            .encode(
                hist,
                e ->
                    t.eq(e.history().initialTransaction())
                        .or(t.in(e.history().initialTransaction().join(e.vis())))
                        .forAll(t.oneOf(e.history().transactions()))
                        .not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void causalityViolationIsDisallowedByCC() {
    History hist =
        new History(
            new Session(new Transaction(1, Arrays.asList(writeOf(0, 1), writeOf(1, 1)))),
            new Session(new Transaction(2, Arrays.asList(readOf(1, 1), writeOf(1, 2)))),
            new Session(new Transaction(3, Arrays.asList(readOf(1, 2), readOf(0, 0)))));
    KodkodProblem p = encoder().encode(hist, CeroneDefinitions.CC);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void causalityViolationIsDisallowedByCCInstanceEncoding() {
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(Arrays.asList("t0", "t1", "t2"));
    atoms.addAll(Arrays.asList(0, 1));
    atoms.addAll(Arrays.asList("x"));
    atoms.addAll(Arrays.asList("s1"));
    Universe universe = new Universe(atoms);
    Instance instance = new Instance(universe);
    TupleFactory tf = universe.factory();
    instance.add(transactions, tf.setOf("t0", "t1", "t2"));
    instance.add(keys, tf.setOf("x"));
    instance.add(values, tf.setOf(0, 1));
    instance.add(sessions, tf.setOf("s1"));
    instance.add(initialTransaction, tf.setOf("t0"));
    instance.add(writes, tf.setOf(tf.tuple("t0", "x", 0), tf.tuple("t1", "x", 1)));
    instance.add(reads, tf.setOf(tf.tuple("t1", "x", 0), tf.tuple("t2", "x", 0)));
    instance.add(sessionOrder, tf.setOf(tf.tuple("t1", "t2")));
    instance.add(txn_session, tf.setOf(tf.tuple("t1", "s1"), tf.tuple("t2", "s1")));
    KodkodProblem p =
        encoder()
            .encode(
                DirectAbstractHistoryEncoding.instance(),
                instance,
                CeroneDefinitions.EXT.and(CeroneDefinitions.SESSION));
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  default void causalityViolationIsDisallowedByCCInstanceEncoding2() {
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(Arrays.asList("t0", "t1", "t2", "t3"));
    atoms.addAll(Arrays.asList(0, 1, 2));
    atoms.addAll(Arrays.asList("x", "y"));
    atoms.addAll(Arrays.asList("s1", "s2"));
    Universe universe = new Universe(atoms);
    Instance instance = new Instance(universe);
    TupleFactory tf = universe.factory();
    instance.add(transactions, tf.setOf("t0", "t1", "t2", "t3"));
    instance.add(keys, tf.setOf("x", "y"));
    instance.add(values, tf.setOf(0, 1));
    instance.add(sessions, tf.setOf("s1", "s2"));
    instance.add(initialTransaction, tf.setOf("t0"));
    instance.add(writes, tf.setOf(tf.tuple("t0", "x", 0), tf.tuple("t1", "x", 1)));
    instance.add(reads, tf.setOf(tf.tuple("t1", "x", 0), tf.tuple("t2", "x", 0)));
    instance.add(sessionOrder, tf.setOf(tf.tuple("t1", "t2")));
    instance.add(txn_session, tf.setOf(tf.tuple("t1", "s1"), tf.tuple("t2", "s1")));
    KodkodProblem p =
        encoder()
            .encode(
                DirectAbstractHistoryEncoding.instance(),
                instance,
                CeroneDefinitions.EXT.and(CeroneDefinitions.SESSION));
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void causalityViolationIsAllowedByRA() {
    History hist =
        new History(
            new Session(new Transaction(1, Arrays.asList(writeOf(0, 1), writeOf(1, 1)))),
            new Session(new Transaction(2, Arrays.asList(readOf(1, 1), writeOf(1, 2)))),
            new Session(new Transaction(3, Arrays.asList(readOf(1, 2), readOf(0, 0)))));
    KodkodProblem p = encoder().encode(hist, CeroneDefinitions.RA);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.sat());
  }

  @Test
  default void txnMustBeAwareOfPrecedingTxnInSesssionOrderInRA() {
    History hist =
        new History(
            new Session(
                new Transaction(1, Arrays.asList(readOf(1, 0), writeOf(0, 2), writeOf(1, 1))),
                new Transaction(2, Arrays.asList(readOf(1, 0), writeOf(0, 2), writeOf(1, 1)))),
            new Session(
                new Transaction(3, Arrays.asList(readOf(1, 0), writeOf(0, 2), writeOf(1, 1)))));
    KodkodProblem p = encoder().encode(hist, CeroneDefinitions.RA);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }
}
