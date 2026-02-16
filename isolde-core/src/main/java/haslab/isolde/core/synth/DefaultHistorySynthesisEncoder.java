package haslab.isolde.core.synth;

import static haslab.isolde.core.DirectAbstractHistoryEncoding.*;

import haslab.isolde.core.*;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.synth.FolSynthesisProblem.InputWithTotalOrder;
import haslab.isolde.kodkod.KodkodUtil;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public final class DefaultHistorySynthesisEncoder implements HistoryEncoder<InputWithTotalOrder> {

  @Override
  public AbstractHistoryRel encoding() {
    return DirectAbstractHistoryEncoding.instance();
  }

  @Override
  public Formula encode(InputWithTotalOrder inputWithTotalOrder, Bounds b) {
    FolSynthesisInput input = inputWithTotalOrder.input();
    TupleSet txnTotalOrderTs = inputWithTotalOrder.totalOrder();
    HistoryAtoms historyAtoms = input.historyAtoms();
    HistoryFormula histFormula = input.historyFormula();
    TupleFactory f = b.universe().factory();

    b.boundExactly(transactions, f.setOf(historyAtoms.getTxnAtoms().toArray()));
    b.boundExactly(keys, f.setOf(historyAtoms.getObjAtoms().toArray()));
    b.boundExactly(values, f.setOf(historyAtoms.getValAtoms().toArray()));
    b.boundExactly(initialTransaction, f.setOf(historyAtoms.initialTxn()));

    TupleSet writesLowerBound =
        f.setOf(historyAtoms.initialTxn())
            .product(f.setOf(historyAtoms.getObjAtoms().toArray()))
            .product(f.setOf(historyAtoms.getValAtoms().get(0)));
    TupleSet writesUpperBound =
        f.setOf(historyAtoms.normalTxns().toArray())
            .product(f.setOf(historyAtoms.getObjAtoms().toArray()))
            .product(f.setOf(historyAtoms.normalValues().toArray()));
    writesUpperBound.addAll(writesLowerBound);
    b.bound(writes, writesLowerBound, writesUpperBound);

    TupleSet readsUpperBound =
        f.setOf(historyAtoms.normalTxns().toArray())
            .product(f.setOf(historyAtoms.getObjAtoms().toArray()))
            .product(f.setOf(historyAtoms.getValAtoms().toArray()));
    b.bound(reads, readsUpperBound);

    TupleSet sessionOrderLowerBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));

    b.bound(sessionOrder, sessionOrderLowerBound, txnTotalOrderTs);

    Relation txnTotalOrderRel = Relation.binary("Txn total order");
    b.boundExactly(txnTotalOrderRel, txnTotalOrderTs);

    return Formula.and(
        histFormula.resolve(this.encoding()),
        encoding().binaryWr().in(txnTotalOrderRel), // TODO: this should be in some other place.
        noBlindWrites(),
        noEmptyTransactions(),
        transactionsWriteToKeyAtMostOnce(),
        transactionsReadKeyAtMostOnce(),
        KodkodUtil.transitive(this.encoding().sessionOrder()),
        this.encoding().noReadsFromThinAir(),
        uniqueWrites());
  }

  private Formula noEmptyTransactions() {
    var e = DirectAbstractHistoryEncoding.instance();
    return e.finalWrites()
        .union(e.externalReads())
        .join(e.values())
        .join(e.keys())
        .eq(e.transactions());
  }

  private Formula noBlindWrites() {
    Variable t = Variable.unary("t");
    return DirectAbstractHistoryEncoding.instance()
        .writeSet(t)
        .in(DirectAbstractHistoryEncoding.instance().readSet(t))
        .forAll(t.oneOf(DirectAbstractHistoryEncoding.instance().normalTxns()));
  }

  private Formula transactionsWriteToKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");
    return x.join(t.join(writes)).lone().forAll(t.oneOf(transactions).and(x.oneOf(keys)));
  }

  private Formula transactionsReadKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");
    return x.join(t.join(reads)).lone().forAll(t.oneOf(transactions).and(x.oneOf(keys)));
  }

  private Formula uniqueWrites() {
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    return writes.join(n).join(x).lone().forAll(x.oneOf(keys).and(n.oneOf(values)));
  }
}
