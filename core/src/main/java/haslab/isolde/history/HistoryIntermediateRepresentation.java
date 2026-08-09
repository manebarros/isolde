package haslab.isolde.history;

import static haslab.isolde.kodkod.Translations.readBinaryExpression;
import static haslab.isolde.kodkod.Translations.readUnaryExpression;

import haslab.isolde.core.HistorySchema;
import haslab.isolde.kodkod.Atom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kodkod.engine.Evaluator;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

/**
 * Turns a solved instance into a {@link History}.
 *
 * <p>The initial transaction does not survive the translation. It is an artifact of the Kodkod
 * encoding — it exists to give every object a value that can be read before anything writes one —
 * and outside the model only the normal transactions are part of the history. Every other consumer
 * already agrees: hand-written histories start at transaction 1, {@code
 * CheckingIntermediateRepresentation} manufactures its own initial transaction when encoding one
 * back, and the executions print their commit / visibility orders over the normal transactions
 * only. Dropping it also drops the {@code initial -> t} session-order edges, which are the
 * encoder's lower bound and say nothing about the history.
 */
public class HistoryIntermediateRepresentation {
  private final TupleSet readsTs;
  private final TupleSet writesTs;
  private final Set<Integer> normalTxns;

  /** Session order over the normal transactions. Transitive, as the synthesis encoder enforces. */
  private final Map<Integer, Set<Integer>> so;

  public HistoryIntermediateRepresentation(HistorySchema encoding, Instance instance) {
    Evaluator evaluator = new Evaluator(instance);
    this.readsTs = evaluator.evaluate(encoding.externalReads());
    this.writesTs = evaluator.evaluate(encoding.finalWrites());
    this.normalTxns = readUnaryExpression(evaluator, encoding.normalTxns(), Integer.class);
    this.so = new LinkedHashMap<>();
    readBinaryExpression(evaluator, encoding.sessionOrder(), Integer.class, Integer.class)
        .forEach(
            (from, tos) -> {
              if (!normalTxns.contains(from)) return;
              Set<Integer> successors = new LinkedHashSet<>(tos);
              successors.retainAll(normalTxns);
              if (!successors.isEmpty()) so.put(from, successors);
            });
  }

  public History buildHistory() {
    Map<Integer, List<Operation>> operations = decodeTransactions();

    List<Session> sessions = new ArrayList<>();
    for (List<Integer> ids : sessions()) {
      List<Transaction> transactions = new ArrayList<>(ids.size());
      for (Integer tid : ids) transactions.add(new Transaction(tid, operations.get(tid)));

      Set<List<Integer>> order = new LinkedHashSet<>();
      for (int i = 0; i < ids.size(); i++) {
        for (int j = 0; j < ids.size(); j++) {
          if (successorsOf(ids.get(i)).contains(ids.get(j))) order.add(List.of(i, j));
        }
      }
      sessions.add(new Session(transactions, order));
    }
    return new History(sessions);
  }

  /**
   * Splits the transactions into sessions: the connected components of the session order, each
   * listed in an order the session order agrees with.
   *
   * <p>Components are the right grouping because every session-order edge lies inside one, so no
   * edge is lost and none crosses a session boundary. The previous implementation instead placed
   * each transaction next to whichever of its successors happened to have been visited already, and
   * opened a new session otherwise — which split a chain whenever the successor came first in the
   * map's iteration order.
   */
  private List<List<Integer>> sessions() {
    Map<Integer, Integer> componentOf = new LinkedHashMap<>();
    for (Integer tid : normalTxns) componentOf.put(tid, tid);
    boolean merged = true;
    while (merged) {
      merged = false;
      for (var edges : so.entrySet()) {
        for (Integer to : edges.getValue()) {
          int a = componentOf.get(edges.getKey());
          int b = componentOf.get(to);
          if (a == b) continue;
          int winner = Math.min(a, b);
          int loser = Math.max(a, b);
          componentOf.replaceAll((tid, component) -> component == loser ? winner : component);
          merged = true;
        }
      }
    }

    Map<Integer, List<Integer>> grouped = new LinkedHashMap<>();
    for (Integer tid : normalTxns) {
      grouped.computeIfAbsent(componentOf.get(tid), component -> new ArrayList<>()).add(tid);
    }

    // Session order is transitive, so t -> s implies the successors of s are a proper subset of
    // those of t: ordering by descending out-degree is therefore always a topological order. Ties
    // break by id, and sessions by their first transaction, so the output does not depend on the
    // order Kodkod happens to hand back tuples in.
    Comparator<Integer> topological =
        Comparator.<Integer>comparingInt(tid -> -successorsOf(tid).size())
            .thenComparingInt(tid -> tid);
    List<List<Integer>> sessions = new ArrayList<>();
    for (List<Integer> session : grouped.values()) {
      session.sort(topological);
      sessions.add(session);
    }
    sessions.sort(Comparator.comparingInt(session -> session.get(0)));
    return sessions;
  }

  private Set<Integer> successorsOf(Integer tid) {
    return so.getOrDefault(tid, Set.of());
  }

  private Map<Integer, List<Operation>> decodeTransactions() {
    Map<Integer, List<Operation>> transactions = new LinkedHashMap<>();
    // Seeded with every normal transaction, so that one without operations still shows up and so
    // that the initial transaction — which appears among the writes, having written every initial
    // value — is left out.
    for (Integer tid : normalTxns) transactions.put(tid, new ArrayList<>());

    for (var tuple : readsTs) {
      Integer tid = Integer.class.cast(((Atom<?>) tuple.atom(0)).value());
      if (!normalTxns.contains(tid)) continue;
      Integer key = Integer.class.cast(((Atom<?>) tuple.atom(1)).value());
      Integer val = Integer.class.cast(((Atom<?>) tuple.atom(2)).value());
      transactions.get(tid).add(Operation.readOf(key, val));
    }

    for (var tuple : writesTs) {
      Integer tid = Integer.class.cast(((Atom<?>) tuple.atom(0)).value());
      if (!normalTxns.contains(tid)) continue;
      Integer key = Integer.class.cast(((Atom<?>) tuple.atom(1)).value());
      Integer val = Integer.class.cast(((Atom<?>) tuple.atom(2)).value());
      transactions.get(tid).add(Operation.writeOf(key, val));
    }
    return transactions;
  }
}
