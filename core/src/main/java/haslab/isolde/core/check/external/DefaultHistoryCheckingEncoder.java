package haslab.isolde.core.check.external;

import static haslab.isolde.core.DirectAbstractHistoryEncoding.initialTransaction;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.keys;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.reads;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.sessionOrder;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.transactions;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.values;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.writes;
import static haslab.isolde.kodkod.KodkodUtil.asTupleSet;

import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.history.AbstractTransaction;
import haslab.isolde.kodkod.Atom;
import java.util.LinkedHashSet;
import java.util.Set;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class DefaultHistoryCheckingEncoder
    implements HistoryEncoder<CheckingIntermediateRepresentation> {

  private DefaultHistoryCheckingEncoder() {}

  private static DefaultHistoryCheckingEncoder instance = null;

  public static DefaultHistoryCheckingEncoder instance() {
    if (instance == null) {
      instance = new DefaultHistoryCheckingEncoder();
    }
    return instance;
  }

  @Override
  public AbstractHistoryRel encoding() {
    return DirectAbstractHistoryEncoding.instance();
  }

  @Override
  public Formula encode(CheckingIntermediateRepresentation history, Bounds b) {
    TupleFactory f = b.universe().factory();
    TupleSet txnTs = asTupleSet(f, history.normalTxnAtoms());
    txnTs.add(f.tuple(history.getInitialTxnAtom()));

    b.boundExactly(transactions, txnTs);
    b.boundExactly(keys, asTupleSet(f, history.getKeyAtoms().values()));
    b.boundExactly(values, asTupleSet(f, history.getValAtoms().values()));
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

    for (int sid = 0; sid < history.getHistory().getSessions().size(); sid++) {
      var session = history.getHistory().getSessions().get(sid);
      Set<Atom<Integer>> prevTxn = new LinkedHashSet<>();
      for (int i = 0; i < session.transactions().size(); i++) {
        Atom<Integer> txnAtom = history.getTxnAtoms().get(sid).get(i);
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

    return Formula.TRUE;
  }
}
