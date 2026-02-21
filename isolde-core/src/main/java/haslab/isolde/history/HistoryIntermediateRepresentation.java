package haslab.isolde.history;

import static haslab.isolde.kodkod.Util.readBinaryExpression;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.kodkod.Atom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kodkod.engine.Evaluator;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

// TODO: test this class
public class HistoryIntermediateRepresentation {
  private TupleSet readsTs;
  private TupleSet writesTs;
  private Map<Integer, Set<Integer>> so;

  public HistoryIntermediateRepresentation(AbstractHistoryK encoding, Instance instance) {
    Evaluator evaluator = new Evaluator(instance);
    this.readsTs = evaluator.evaluate(encoding.externalReads());
    this.writesTs = evaluator.evaluate(encoding.finalWrites());
    this.so =
        readBinaryExpression(evaluator, encoding.sessionOrder(), Integer.class, Integer.class);
  }

  // TODO: test this
  public History buildHistory() {
    Map<Integer, List<Operation>> ts = decodeTransactions();
    List<List<Integer>> soTranslated = translate(ts.keySet());

    List<Session> sessions = new ArrayList<>();
    for (var list : soTranslated) {
      List<Transaction> sessionTransactions = new ArrayList<>();
      for (var tid : list) {
        sessionTransactions.add(new Transaction(tid, ts.get(tid)));
      }
      sessions.add(new Session(sessionTransactions));
    }

    return new History(sessions);
  }

  // TODO: test this
  private List<List<Integer>> translate(Set<Integer> tids) {
    Map<Integer, Integer> txn_session = new LinkedHashMap<>();
    List<Set<Integer>> sessionSets = new ArrayList<>();
    int nextSession = 0;

    for (var tid : tids) {
      boolean inExistingSession = false;
      if (this.so.containsKey(tid)) {
        for (var followingTid : this.so.get(tid)) {
          if (txn_session.containsKey(followingTid)) {
            inExistingSession = true;
            var session = txn_session.get(followingTid);
            txn_session.put(tid, session);
            sessionSets.get(session).add(tid);
            break;
          }
        }
      }
      if (!inExistingSession) {
        txn_session.put(tid, nextSession++);
        Set<Integer> newSession = new LinkedHashSet<>();
        newSession.add(tid);
        sessionSets.add(newSession);
      }
    }

    return sessionSets.stream()
        .map(
            sessionSet ->
                sessionSet.stream()
                    .sorted(
                        (a, b) -> {
                          int sizeA = so.getOrDefault(a, Collections.emptySet()).size();
                          int sizeB = so.getOrDefault(b, Collections.emptySet()).size();
                          return Integer.compare(
                              sizeB, sizeA); // descending order (bigger sets first)
                        })
                    .collect(Collectors.toList()))
        .collect(Collectors.toList());
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
