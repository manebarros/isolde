package com.github.manebarros;

import static com.github.manebarros.DirectAbstractHistoryEncoding.*;
import static com.github.manebarros.KodkodUtil.asTupleSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public final class CeroneCheckingEncoder implements CheckingEncoder<CeroneExecutionK> {

  private CeroneCheckingEncoder() {}

  private static CeroneCheckingEncoder instance = null;

  public static CeroneCheckingEncoder instance() {
    if (instance == null) {
      instance = new CeroneCheckingEncoder();
    }
    return instance;
  }

  @Override
  public CheckingContextualized<CeroneExecutionK, KodkodProblem> encode(
      AbstractHistoryK encoding, Instance instance, ExecutionFormulaK<CeroneExecutionK> formula) {
    Relation arAux = Relation.binary("Arbitration's transitive reduction");
    Relation vis = Relation.binary("Vis");
    return this.encode(encoding, instance, formula, arAux, vis);
  }

  public CheckingContextualized<CeroneExecutionK, KodkodProblem> encode(
      AbstractHistoryK encoding,
      Instance instance,
      ExecutionFormulaK<CeroneExecutionK> formula,
      Relation arAux,
      Relation vis) {
    Universe u = instance.universe();
    Bounds b = new Bounds(u);
    TupleFactory f = u.factory();
    Evaluator ev = new Evaluator(instance);
    b.boundExactly(transactions, ev.evaluate(encoding.transactions()));
    b.boundExactly(keys, ev.evaluate(encoding.keys()));
    b.boundExactly(values, ev.evaluate(encoding.values()));
    b.boundExactly(sessions, ev.evaluate(encoding.sessions()));
    b.boundExactly(initialTransaction, ev.evaluate(encoding.initialTransaction()));
    b.boundExactly(writes, ev.evaluate(encoding.finalWrites()));
    b.boundExactly(reads, ev.evaluate(encoding.externalReads()));
    b.boundExactly(sessionOrder, ev.evaluate(encoding.sessionOrder()));
    b.boundExactly(txn_session, ev.evaluate(encoding.txn_session()));

    Relation lastTxn = Relation.unary("last txn");

    TupleSet arLowerBound =
        ev.evaluate(encoding.initialTransaction()).product(ev.evaluate(encoding.normalTxns()));

    TupleSet arUpperBound =
        Util.irreflexiveBound(f, Util.unaryTupleSetToAtoms(ev.evaluate(encoding.normalTxns())));
    arUpperBound.addAll(arLowerBound);

    b.bound(vis, arLowerBound, arUpperBound);
    b.bound(arAux, arUpperBound);
    // b.bound(
    //    commitOrder,
    //    commitOrderLowerBound,
    //    commitOrderUpperBound); // TODO: Improve using info from so + wr
    b.bound(lastTxn, ev.evaluate(encoding.normalTxns()));

    Expression ar = arAux.closure();

    CeroneExecutionK exec = CeroneExecutionK.build(vis, ar);

    var enc = DirectAbstractHistoryEncoding.instance();
    Formula spec =
        Formula.and(
            vis.in(ar),
            enc.sessionOrder().in(ar),
            arAux.totalOrder(transactions, initialTransaction, lastTxn),
            formula.apply(enc, exec));

    return new CheckingContextualized<>(enc, exec, new KodkodProblem(spec, b));
  }

  @Override
  public CheckingContextualized<CeroneExecutionK, KodkodProblem> encode(
      History history, ExecutionFormulaK<CeroneExecutionK> formula) {
    Relation vis = Relation.binary("Vis");
    Relation arAux = Relation.binary("CO's transitive reduction");
    return this.encode(history, formula, arAux, vis);
  }

  public CheckingContextualized<CeroneExecutionK, KodkodProblem> encode(
      History history, ExecutionFormulaK<CeroneExecutionK> formula, Relation arAux, Relation vis) {
    List<Atom<Integer>> sessAtoms = new ArrayList<>();
    List<List<Atom<Integer>>> txnAtoms = new ArrayList<>();
    Map<Integer, Atom<Integer>> keyAtoms = new LinkedHashMap<>();
    Map<Integer, Atom<Integer>> valAtoms = new LinkedHashMap<>();
    Atom<Integer> initialTxnAtom = new Atom<Integer>("t", 0);
    valAtoms.put(0, new Atom<>("v", 0));

    int nextTid = 1;
    int nextSid = 0;
    for (var session : history.getSessions()) {
      sessAtoms.add(new Atom<>("s", nextSid++));
      List<Atom<Integer>> sessTxnAtoms = new ArrayList<>();
      txnAtoms.add(sessTxnAtoms);
      for (var txn : session.transactions()) {
        sessTxnAtoms.add(new Atom<>("t", nextTid++));
        for (var op : txn.operations()) {
          var key = op.object();
          var val = op.value();
          if (!keyAtoms.containsKey(key)) {
            keyAtoms.put(key, new Atom<>("x", key));
          }
          if (!valAtoms.containsKey(val)) {
            valAtoms.put(val, new Atom<>("v", val));
          }
        }
      }
    }
    List<Atom<Integer>> normalTxnAtoms =
        txnAtoms.stream().flatMap(s -> s.stream()).collect(Collectors.toList());

    List<Object> allAtoms = new ArrayList<>();
    allAtoms.addAll(normalTxnAtoms);
    allAtoms.add(initialTxnAtom);
    allAtoms.addAll(keyAtoms.values());
    allAtoms.addAll(valAtoms.values());
    allAtoms.addAll(sessAtoms);

    Universe u = new Universe(allAtoms);
    TupleFactory f = u.factory();
    Bounds b = new Bounds(u);

    TupleSet txnTs = asTupleSet(f, normalTxnAtoms);
    txnTs.add(f.tuple(initialTxnAtom));

    b.boundExactly(transactions, txnTs);
    b.boundExactly(keys, asTupleSet(f, keyAtoms.values()));
    b.boundExactly(values, asTupleSet(f, valAtoms.values()));
    b.boundExactly(sessions, asTupleSet(f, sessAtoms));
    b.boundExactly(initialTransaction, f.setOf(initialTxnAtom));

    TupleSet writesTs = f.noneOf(3);
    writesTs.addAll(
        f.setOf(initialTxnAtom)
            .product(asTupleSet(f, keyAtoms.values()))
            .product(f.setOf(valAtoms.get(0))));
    TupleSet readsTs = f.noneOf(3);
    TupleSet soTs = f.noneOf(2);
    soTs.addAll(f.setOf(initialTxnAtom).product(asTupleSet(f, normalTxnAtoms)));
    TupleSet txn_sessionTs = f.noneOf(2);

    for (int sid = 0; sid < history.getSessions().size(); sid++) {
      var session = history.getSessions().get(sid);
      Set<Atom<Integer>> prevTxn = new LinkedHashSet<>();
      for (int i = 0; i < session.transactions().size(); i++) {
        Atom<Integer> txnAtom = txnAtoms.get(sid).get(i);
        txn_sessionTs.add(f.tuple(txnAtom, sessAtoms.get(sid)));
        for (var atom : prevTxn) {
          soTs.add(f.tuple(atom, txnAtom));
        }
        prevTxn.add(txnAtom);
        AbstractTransaction at = new AbstractTransaction(session.transactions().get(i));
        for (var key : at.getReads().keySet()) {
          readsTs.add(f.tuple(txnAtom, keyAtoms.get(key), valAtoms.get(at.getReads().get(key))));
        }
        for (var key : at.getWrites().keySet()) {
          writesTs.add(f.tuple(txnAtom, keyAtoms.get(key), valAtoms.get(at.getWrites().get(key))));
        }
      }
    }
    b.boundExactly(writes, writesTs);
    b.boundExactly(reads, readsTs);
    b.boundExactly(sessionOrder, soTs);
    b.boundExactly(txn_session, txn_sessionTs);

    Relation lastTxn = Relation.unary("last txn");

    TupleSet arLowerBound = b.lowerBound(sessionOrder); // I think we always want so \subseteq ar
    TupleSet arUpperBound = Util.irreflexiveBound(f, normalTxnAtoms);
    arUpperBound.addAll(arLowerBound);

    var encoding = DirectAbstractHistoryEncoding.instance();

    b.bound(
        vis,
        f.setOf(initialTxnAtom).product(asTupleSet(f, normalTxnAtoms)),
        arUpperBound); // We allow so \not\subseteq vis (however, I suspect all levels disallow it)
    b.bound(arAux, arUpperBound);
    b.bound(lastTxn, asTupleSet(f, normalTxnAtoms));

    Expression ar = arAux.closure(); // Maybe using a relational variable would be more efficient

    CeroneExecutionK exec = CeroneExecutionK.build(vis, ar);

    Formula spec =
        Formula.and(
            vis.in(ar),
            encoding.sessionOrder().in(ar),
            arAux.totalOrder(transactions, initialTransaction, lastTxn),
            formula.apply(encoding, exec));

    return new CheckingContextualized<>(encoding, exec, new KodkodProblem(spec, b));
  }
}
