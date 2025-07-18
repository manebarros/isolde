package haslab.isolde.core.synth.noSession;

import static haslab.isolde.core.DirectAbstractHistoryEncoding.initialTransaction;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.keys;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.reads;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.sessionOrder;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.sessions;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.transactions;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.txn_session;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.values;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.writes;

import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.kodkod.Atom;
import haslab.isolde.kodkod.KodkodUtil;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public final class DefaultSimpleHistorySynthesisEncoder
    implements HistoryEncoder<FolSynthesisInput, TupleSet> {

  @Override
  public AbstractHistoryRel encoding() {
    return DirectAbstractHistoryEncoding.instance();
  }

  @Override
  public Formula encode(FolSynthesisInput input, TupleSet txnTotalOrderTs, Bounds b) {
    HistoryAtoms historyAtoms = input.historyAtoms();
    HistoryFormula histFormula = input.historyFormula();
    TupleFactory f = b.universe().factory();

    b.boundExactly(transactions, f.setOf(historyAtoms.getTxnAtoms().toArray()));
    b.boundExactly(keys, f.setOf(historyAtoms.getObjAtoms().toArray()));
    b.boundExactly(values, f.setOf(historyAtoms.getValAtoms().toArray()));
    b.boundExactly(sessions, f.setOf(historyAtoms.getSessionAtoms().toArray()));
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

    TupleSet sessionOrderExactBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));
    b.boundExactly(sessionOrder, sessionOrderExactBound);

    TupleSet txnSessionExactBound = f.noneOf(2);
    for (int i = 0; i < historyAtoms.normalTxns().size(); i++) {
      Atom<Integer> txnAtom = historyAtoms.normalTxns().get(i);
      Atom<Integer> sessAtom = historyAtoms.getSessionAtoms().get(i);
      txnSessionExactBound.add(f.tuple(txnAtom, sessAtom));
    }
    b.boundExactly(txn_session, txnSessionExactBound);

    Relation txnTotalOrderRel = Relation.binary("Txn total order");
    b.boundExactly(txnTotalOrderRel, txnTotalOrderTs);

    return Formula.and(
        histFormula.resolve(this.encoding()),
        encoding().binaryWr().in(txnTotalOrderRel),
        // encoding().sessionOrder().union(encoding().binaryWr()).in(txnTotalOrderRel),
        // KodkodUtil.acyclic(encoding().binaryWr().union(encoding().sessionOrder())),
        noBlindWrites(),
        noEmptyTransactions(),
        transactionsWriteToKeyAtMostOnce(),
        transactionsReadKeyAtMostOnce(),
        // sessionSemantics(),
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

  private Formula sessionSemantics() {
    Variable s = Variable.unary("s");
    Expression normalTxns = DirectAbstractHistoryEncoding.instance().normalTxns();

    return Formula.and(
        txn_session.function(normalTxns, sessions),
        txn_session.transpose().join(sessionOrder.join(txn_session)).in(Expression.IDEN),
        KodkodUtil.total(sessionOrder, txn_session.join(s)).forAll(s.oneOf(sessions)),
        KodkodUtil.transitive(sessionOrder));
  }

  private Formula uniqueWrites() {
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    return writes.join(n).join(x).lone().forAll(x.oneOf(keys).and(n.oneOf(values)));
  }
}
