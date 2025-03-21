package com.github.manebarros.generic;

import static com.github.manebarros.DirectAbstractHistoryEncoding.initialTransaction;
import static com.github.manebarros.DirectAbstractHistoryEncoding.keys;
import static com.github.manebarros.DirectAbstractHistoryEncoding.reads;
import static com.github.manebarros.DirectAbstractHistoryEncoding.sessionOrder;
import static com.github.manebarros.DirectAbstractHistoryEncoding.sessions;
import static com.github.manebarros.DirectAbstractHistoryEncoding.transactions;
import static com.github.manebarros.DirectAbstractHistoryEncoding.txn_session;
import static com.github.manebarros.DirectAbstractHistoryEncoding.values;
import static com.github.manebarros.DirectAbstractHistoryEncoding.writes;
import static com.github.manebarros.KodkodUtil.total;
import static com.github.manebarros.KodkodUtil.transitive;

import com.github.manebarros.DirectAbstractHistoryEncoding;
import com.github.manebarros.KodkodProblem;
import com.github.manebarros.KodkodUtil;
import com.github.manebarros.Scope;
import java.util.ArrayList;
import java.util.List;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public class FolSynthesisEncoder {
  private final HistoryAtoms historyAtoms;
  private final Formula historyFormula;
  private final List<ProblemExtender> extenders;
  private final List<Object> atoms;

  public FolSynthesisEncoder(Scope scope, Formula historyFormula) {
    this.historyAtoms = new HistoryAtoms(scope);
    this.historyFormula = historyFormula;
    this.extenders = new ArrayList<>();
    this.atoms = new ArrayList<>();
  }

  public <E extends Execution> List<E> add(
      List<ExecutionFormula<E>> formulas, SynthesisEncoder<E> encoder) {
    SynthesisModule<E> module = encoder.getModule(formulas, historyAtoms);
    this.extenders.add(module.extender());
    this.atoms.addAll(module.atoms());
    return module.executions();
  }

  public KodkodProblem encode() {
    Universe u = new Universe(this.atoms);
    Bounds b = new Bounds(u);
    TupleFactory f = u.factory();

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

    TupleSet txnTotalOrderTs = f.noneOf(2);
    // Traverse the txn indexes from the initial txn (i=0) to the penultimate txn
    for (int i = 0; i < historyAtoms.getTxnAtoms().size() - 1; i++) {
      for (int j = i + 1; j < historyAtoms.getTxnAtoms().size() - 1; j++) {
        txnTotalOrderTs.add(
            f.tuple(historyAtoms.getTxnAtoms().get(i), historyAtoms.getTxnAtoms().get(j)));
      }
    }

    TupleSet sessionOrderLowerBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));

    b.bound(sessionOrder, sessionOrderLowerBound, txnTotalOrderTs);
    b.bound(
        txn_session,
        f.setOf(historyAtoms.normalTxns().toArray())
            .product(f.setOf(historyAtoms.getSessionAtoms().toArray())));

    var enc = DirectAbstractHistoryEncoding.instance();
    Formula formula =
        Formula.and(
            historyFormula,
            noBlindWrites(),
            noEmptyTransactions(),
            transactionsWriteToKeyAtMostOnce(),
            transactionsReadKeyAtMostOnce(),
            sessionSemantics(),
            enc.noReadsFromThinAir(),
            uniqueWrites());

    formula = formula.and(this.extenders.get(0).extend(b, txnTotalOrderTs));
    for (int i = 1; i < this.extenders.size(); i++) {
      formula = formula.and(this.extenders.get(i).extend(b));
    }

    return new KodkodProblem(formula, b);
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
        transitive(sessionOrder));
  }

  private Formula uniqueWrites() {
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    return writes.join(n).join(x).lone().forAll(x.oneOf(keys).and(n.oneOf(values)));
  }

  private Formula commitOrderSemantics(Expression commitOrder) {
    return Formula.and(
        transitive(commitOrder),
        total(commitOrder, transactions),
        sessionOrder.union(DirectAbstractHistoryEncoding.instance().binaryWr()).in(commitOrder));
  }
}
