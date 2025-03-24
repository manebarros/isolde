package com.github.manebarros.core;

import static com.github.manebarros.kodkod.Util.readBinaryExpression;
import static com.github.manebarros.kodkod.Util.readUnaryExpression;

import com.github.manebarros.history.History;
import com.github.manebarros.history.Operation;
import com.github.manebarros.history.Session;
import com.github.manebarros.history.Transaction;
import com.github.manebarros.kodkod.Atom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kodkod.engine.Evaluator;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

public class HistoryIntermediateRepresentation {
  private Set<Integer> sessions;

  private TupleSet readsTs;
  private TupleSet writesTs;

  private Map<Integer, Set<Integer>> sessionOrder;
  private Map<Integer, Set<Integer>> session_txn;

  public HistoryIntermediateRepresentation(AbstractHistoryK encoding, Instance instance) {
    Evaluator evaluator = new Evaluator(instance);

    this.sessions = readUnaryExpression(evaluator, encoding.sessions(), Integer.class);

    this.readsTs = evaluator.evaluate(encoding.externalReads());
    this.writesTs = evaluator.evaluate(encoding.finalWrites());

    this.sessionOrder =
        readBinaryExpression(evaluator, encoding.sessionOrder(), Integer.class, Integer.class);

    this.session_txn =
        readBinaryExpression(
            evaluator, encoding.txn_session().transpose(), Integer.class, Integer.class);
  }

  public History buildHistory() {
    Map<Integer, List<Operation>> ts = decodeTransactions();
    return new History(this.sessions.stream().sorted().map(sid -> decodeSession(ts, sid)).toList());
  }

  private Session decodeSession(Map<Integer, List<Operation>> ts, int sid) {
    return new Session(
        this.session_txn.getOrDefault(sid, new HashSet<>()).stream()
            .sorted(
                (t, s) ->
                    sessionOrder.getOrDefault(s, new HashSet<>()).size()
                        - sessionOrder.getOrDefault(t, new HashSet<>()).size())
            .map(tid -> new Transaction(tid, ts.get(tid)))
            .toList());
  }

  private Map<Integer, List<Operation>> decodeTransactions() {
    Map<Integer, List<Operation>> transactions = new LinkedHashMap<>();
    for (var tuple : readsTs) {
      Integer tid = Integer.class.cast(((Atom<?>) tuple.atom(0)).value());
      Integer key = Integer.class.cast(((Atom<?>) tuple.atom(1)).value());
      Integer val = Integer.class.cast(((Atom<?>) tuple.atom(2)).value());
      if (!transactions.containsKey(tid)) {
        transactions.put(tid, new ArrayList<>());
      }
      transactions.get(tid).add(Operation.readOf(key, val));
    }

    for (var tuple : writesTs) {
      Integer tid = Integer.class.cast(((Atom<?>) tuple.atom(0)).value());
      Integer key = Integer.class.cast(((Atom<?>) tuple.atom(1)).value());
      Integer val = Integer.class.cast(((Atom<?>) tuple.atom(2)).value());
      if (!transactions.containsKey(tid)) {
        transactions.put(tid, new ArrayList<>());
      }
      transactions.get(tid).add(Operation.writeOf(key, val));
    }
    return transactions;
  }
}
