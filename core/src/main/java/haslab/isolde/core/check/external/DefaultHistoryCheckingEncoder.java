package haslab.isolde.core.check.external;

import static haslab.isolde.kodkod.Formulas.asTupleSet;

import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.history.AbstractTransaction;
import haslab.isolde.kodkod.Atom;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public final class DefaultHistoryCheckingEncoder
    implements HistoryEncoder<CheckingIntermediateRepresentation> {

  public static final DefaultHistoryCheckingEncoder INSTANCE = new DefaultHistoryCheckingEncoder();

  private DefaultHistoryCheckingEncoder() {}

  @Override
  public DirectAbstractHistoryEncoding encoding() {
    return DirectAbstractHistoryEncoding.INSTANCE;
  }

  @Override
  public Formula encode(CheckingIntermediateRepresentation history, Bounds b) {
    TupleFactory f = b.universe().factory();
    DirectAbstractHistoryEncoding enc = encoding();
    TupleSet txnTs = asTupleSet(f, history.normalTxnAtoms());
    txnTs.add(f.tuple(history.getInitialTxnAtom()));

    b.boundExactly(enc.transactions(), txnTs);
    b.boundExactly(enc.keys(), asTupleSet(f, history.getKeyAtoms().values()));
    b.boundExactly(enc.values(), asTupleSet(f, history.getValAtoms().values()));
    b.boundExactly(enc.initialTransaction(), f.setOf(history.getInitialTxnAtom()));

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
      // Taken from the session's own order rather than from the position in the list, so that a
      // session that is not a chain encodes back to the order it actually has. For a chain the two
      // agree, since the order of a chain relates every transaction to all the later ones.
      for (var edge : session.order()) {
        soTs.add(
            f.tuple(
                history.getTxnAtoms().get(sid).get(edge.get(0)),
                history.getTxnAtoms().get(sid).get(edge.get(1))));
      }
      for (int i = 0; i < session.transactions().size(); i++) {
        Atom<Integer> txnAtom = history.getTxnAtoms().get(sid).get(i);
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
    b.boundExactly(enc.finalWrites(), writesTs);
    b.boundExactly(enc.externalReads(), readsTs);
    b.boundExactly(enc.sessionOrder(), soTs);

    return Formula.TRUE;
  }
}
