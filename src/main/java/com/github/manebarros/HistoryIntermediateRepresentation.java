package com.github.manebarros;

import static com.github.manebarros.Util.readBinaryExpression;
import static com.github.manebarros.Util.readFunction;
import static com.github.manebarros.Util.readUnaryExpression;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import kodkod.engine.Evaluator;
import kodkod.instance.Instance;

public class HistoryIntermediateRepresentation {
  private Set<Integer> transactions;
  private Set<String> events;
  private Set<Integer> objects;
  private Set<Integer> values;
  private Set<Integer> sessions;
  private Set<String> reads;
  private Set<String> writes;
  private Integer initialValue;

  private Map<Integer, Set<String>> txn_events;
  private Map<String, Integer> op_obj;
  private Map<String, Integer> op_val;
  private Map<String, Set<String>> programOrder;
  private Map<Integer, Set<Integer>> sessionOrder;
  private Map<Integer, Set<Integer>> session_txn;

  public HistoryIntermediateRepresentation(Instance instance) {
    Evaluator evaluator = new Evaluator(instance);

    this.transactions = readUnaryExpression(evaluator, HistoryEncoding.Transaction, Integer.class);
    this.events = readUnaryExpression(evaluator, HistoryEncoding.Event, String.class);
    this.objects = readUnaryExpression(evaluator, HistoryEncoding.Object, Integer.class);
    this.values = readUnaryExpression(evaluator, HistoryEncoding.Value, Integer.class);
    this.sessions = readUnaryExpression(evaluator, HistoryEncoding.Session, Integer.class);
    this.reads = readUnaryExpression(evaluator, HistoryEncoding.Read, String.class);
    this.writes = readUnaryExpression(evaluator, HistoryEncoding.Write, String.class);
    this.initialValue =
        readUnaryExpression(evaluator, HistoryEncoding.InitialValue, Integer.class).stream()
            .findFirst()
            .get();
    this.txn_events =
        readBinaryExpression(evaluator, HistoryEncoding.events, Integer.class, String.class);
    this.op_obj = readFunction(evaluator, HistoryEncoding.object, String.class, Integer.class);
    this.op_val = readFunction(evaluator, HistoryEncoding.value, String.class, Integer.class);
    this.programOrder =
        readBinaryExpression(evaluator, HistoryEncoding.programOrder, String.class, String.class);
    this.sessionOrder =
        readBinaryExpression(evaluator, HistoryEncoding.sessionOrder, Integer.class, Integer.class);
    this.session_txn =
        readBinaryExpression(
            evaluator, HistoryEncoding.session.transpose(), Integer.class, Integer.class);
  }

  public History buildHistory() {
    return new History(
        this.initialValue, this.sessions.stream().sorted().map(this::decodeSession).toList());
  }

  private Session decodeSession(int sid) {
    return new Session(
        this.session_txn.getOrDefault(sid, new HashSet<>()).stream()
            .sorted(
                (t, s) ->
                    sessionOrder.getOrDefault(s, new HashSet<>()).size()
                        - sessionOrder.getOrDefault(t, new HashSet<>()).size())
            .map(this::decodeTransaction)
            .toList());
  }

  public Transaction decodeTransaction(int tid) {
    return new Transaction(
        this.txn_events.get(tid).stream()
            .sorted(
                (t, s) ->
                    programOrder.getOrDefault(s, new HashSet<>()).size()
                        - programOrder.getOrDefault(t, new HashSet<>()).size())
            .map(this::decodeOperation)
            .toList());
  }

  public Operation decodeOperation(String oid) {
    int obj = this.op_obj.get(oid);
    int val = this.op_val.get(oid);
    return this.reads.contains(oid) ? Operation.readOf(obj, val) : Operation.writeOf(obj, val);
  }
}
