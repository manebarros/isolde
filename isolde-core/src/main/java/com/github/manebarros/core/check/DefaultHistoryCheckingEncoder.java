package com.github.manebarros.core.check;

import static com.github.manebarros.core.DirectAbstractHistoryEncoding.initialTransaction;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.keys;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.reads;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.sessionOrder;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.sessions;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.transactions;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.txn_session;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.values;
import static com.github.manebarros.core.DirectAbstractHistoryEncoding.writes;
import static com.github.manebarros.kodkod.KodkodUtil.asTupleSet;
import static com.github.manebarros.kodkod.Util.unaryTupleSetToAtoms;

import com.github.manebarros.core.AbstractHistoryK;
import com.github.manebarros.core.DirectAbstractHistoryEncoding;
import com.github.manebarros.core.check.candidate.CandCheckHistoryEncoder;
import com.github.manebarros.core.check.external.CheckingIntermediateRepresentation;
import com.github.manebarros.core.check.external.HistCheckHistoryEncoder;
import com.github.manebarros.history.AbstractTransaction;
import com.github.manebarros.kodkod.Atom;
import com.github.manebarros.kodkod.Util;
import java.util.LinkedHashSet;
import java.util.Set;
import kodkod.ast.Formula;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class DefaultHistoryCheckingEncoder
    implements CandCheckHistoryEncoder, HistCheckHistoryEncoder {

  private DefaultHistoryCheckingEncoder() {}

  private static DefaultHistoryCheckingEncoder instance = null;

  public static DefaultHistoryCheckingEncoder instance() {
    if (instance == null) {
      instance = new DefaultHistoryCheckingEncoder();
    }
    return instance;
  }

  @Override
  public AbstractHistoryK encoding() {
    return DirectAbstractHistoryEncoding.instance();
  }

  @Override
  public Formula encode(Instance instance, AbstractHistoryK context, Bounds b) {
    Evaluator ev = new Evaluator(instance);
    TupleFactory f = b.universe().factory();
    b.boundExactly(
        transactions, asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.transactions()))));
    b.boundExactly(keys, asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.keys()))));
    b.boundExactly(values, asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.values()))));
    b.boundExactly(sessions, asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.sessions()))));
    b.boundExactly(
        initialTransaction,
        asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.initialTransaction()))));

    b.boundExactly(writes, Util.convert(ev, context, AbstractHistoryK::finalWrites, f, 3));
    b.boundExactly(reads, Util.convert(ev, context, AbstractHistoryK::externalReads, f, 3));
    b.boundExactly(sessionOrder, Util.convert(ev, context, AbstractHistoryK::sessionOrder, f, 2));
    b.boundExactly(txn_session, Util.convert(ev, context, AbstractHistoryK::txn_session, f, 2));

    return Formula.TRUE;
  }

  @Override
  public Formula encode(CheckingIntermediateRepresentation history, Bounds b) {
    TupleFactory f = b.universe().factory();
    TupleSet txnTs = asTupleSet(f, history.normalTxnAtoms());
    txnTs.add(f.tuple(history.getInitialTxnAtom()));

    b.boundExactly(transactions, txnTs);
    b.boundExactly(keys, asTupleSet(f, history.getKeyAtoms().values()));
    b.boundExactly(values, asTupleSet(f, history.getValAtoms().values()));
    b.boundExactly(sessions, asTupleSet(f, history.getSessAtoms()));
    b.boundExactly(initialTransaction, f.setOf(history.getInitialTxnAtom()));

    TupleSet writesTs = f.noneOf(3);
    writesTs.addAll(
        f.setOf(history.getInitialTxnAtom())
            .product(asTupleSet(f, history.getKeyAtoms().values()))
            .product(f.setOf(history.getValAtoms().get(0))));
    TupleSet readsTs = f.noneOf(3);
    TupleSet soTs = f.noneOf(2);
    soTs.addAll(
        f.setOf(history.getInitialTxnAtom()).product(asTupleSet(f, history.normalTxnAtoms())));
    TupleSet txn_sessionTs = f.noneOf(2);

    for (int sid = 0; sid < history.getHistory().getSessions().size(); sid++) {
      var session = history.getHistory().getSessions().get(sid);
      Set<Atom<Integer>> prevTxn = new LinkedHashSet<>();
      for (int i = 0; i < session.transactions().size(); i++) {
        Atom<Integer> txnAtom = history.getTxnAtoms().get(sid).get(i);
        txn_sessionTs.add(f.tuple(txnAtom, history.getSessAtoms().get(sid)));
        for (var atom : prevTxn) {
          soTs.add(f.tuple(atom, txnAtom));
        }
        prevTxn.add(txnAtom);
        AbstractTransaction at = new AbstractTransaction(session.transactions().get(i));
        for (var key : at.getReads().keySet()) {
          readsTs.add(
              f.tuple(
                  txnAtom,
                  history.getKeyAtoms().get(key),
                  history.getValAtoms().get(at.getReads().get(key))));
        }
        for (var key : at.getWrites().keySet()) {
          writesTs.add(
              f.tuple(
                  txnAtom,
                  history.getKeyAtoms().get(key),
                  history.getValAtoms().get(at.getWrites().get(key))));
        }
      }
    }
    b.boundExactly(writes, writesTs);
    b.boundExactly(reads, readsTs);
    b.boundExactly(sessionOrder, soTs);
    b.boundExactly(txn_session, txn_sessionTs);

    return Formula.TRUE;
  }
}
