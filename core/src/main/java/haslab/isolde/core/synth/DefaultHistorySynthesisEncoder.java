package haslab.isolde.core.synth;

import haslab.isolde.core.*;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.synth.FolSynthesisProblem.InputWithTotalOrder;
import haslab.isolde.kodkod.Formulas;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public final class DefaultHistorySynthesisEncoder implements HistoryEncoder<InputWithTotalOrder> {

  @Override
  public DirectAbstractHistoryEncoding encoding() {
    return DirectAbstractHistoryEncoding.INSTANCE;
  }

  @Override
  public Formula encode(InputWithTotalOrder inputWithTotalOrder, Bounds b) {
    FolSynthesisInput input = inputWithTotalOrder.input();
    TupleSet txnTotalOrderTs = inputWithTotalOrder.totalOrder();
    HistoryAtoms historyAtoms = input.historyAtoms();
    HistoryFormula histFormula = input.historyFormula();
    TupleFactory f = b.universe().factory();
    DirectAbstractHistoryEncoding enc = encoding();

    b.boundExactly(enc.transactions(), f.setOf(historyAtoms.getTxnAtoms().toArray()));
    b.boundExactly(enc.keys(), f.setOf(historyAtoms.getObjAtoms().toArray()));
    b.boundExactly(enc.values(), f.setOf(historyAtoms.getValAtoms().toArray()));
    b.boundExactly(enc.initialTransaction(), f.setOf(historyAtoms.initialTxn()));

    TupleSet writesLowerBound =
        f.setOf(historyAtoms.initialTxn())
            .product(f.setOf(historyAtoms.getObjAtoms().toArray()))
            .product(f.setOf(historyAtoms.getValAtoms().get(0)));
    TupleSet writesUpperBound =
        f.setOf(historyAtoms.normalTxns().toArray())
            .product(f.setOf(historyAtoms.getObjAtoms().toArray()))
            .product(f.setOf(historyAtoms.normalValues().toArray()));
    writesUpperBound.addAll(writesLowerBound);
    b.bound(enc.finalWrites(), writesLowerBound, writesUpperBound);

    TupleSet readsUpperBound =
        f.setOf(historyAtoms.normalTxns().toArray())
            .product(f.setOf(historyAtoms.getObjAtoms().toArray()))
            .product(f.setOf(historyAtoms.getValAtoms().toArray()));
    b.bound(enc.externalReads(), readsUpperBound);

    TupleSet sessionOrderLowerBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));

    b.bound(enc.sessionOrder(), sessionOrderLowerBound, txnTotalOrderTs);

    Relation txnTotalOrderRel = Relation.binary("Txn total order");
    b.boundExactly(txnTotalOrderRel, txnTotalOrderTs);

    return Formula.and(
        histFormula.resolve(enc),
        enc.binaryWr().in(txnTotalOrderRel), // TODO: this should be in some other place.
        noBlindWrites(),
        noEmptyTransactions(),
        transactionsWriteToKeyAtMostOnce(),
        transactionsReadKeyAtMostOnce(),
        Formulas.transitive(enc.sessionOrder()),
        enc.noReadsFromThinAir(),
        uniqueWrites());
  }

  private Formula noEmptyTransactions() {
    var e = encoding();
    return e.finalWrites()
        .union(e.externalReads())
        .join(e.values())
        .join(e.keys())
        .eq(e.transactions());
  }

  private Formula noBlindWrites() {
    Variable t = Variable.unary("t");
    var e = encoding();
    return e.writeSet(t).in(e.readSet(t)).forAll(t.oneOf(e.normalTxns()));
  }

  private Formula transactionsWriteToKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");
    var e = encoding();
    return x.join(t.join(e.finalWrites()))
        .lone()
        .forAll(t.oneOf(e.transactions()).and(x.oneOf(e.keys())));
  }

  private Formula transactionsReadKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");
    var e = encoding();
    return x.join(t.join(e.externalReads()))
        .lone()
        .forAll(t.oneOf(e.transactions()).and(x.oneOf(e.keys())));
  }

  private Formula uniqueWrites() {
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");
    var e = encoding();
    return e.finalWrites()
        .join(n)
        .join(x)
        .lone()
        .forAll(x.oneOf(e.keys()).and(n.oneOf(e.values())));
  }
}
