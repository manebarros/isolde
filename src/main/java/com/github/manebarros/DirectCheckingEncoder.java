package com.github.manebarros;

import static com.github.manebarros.DirectAbstractHistoryEncoding.*;

import java.util.Collections;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public final class DirectCheckingEncoder implements CheckingEncoder {

  private DirectCheckingEncoder() {}

  private static DirectCheckingEncoder instance = null;

  public static DirectCheckingEncoder instance() {
    if (instance == null) {
      instance = new DirectCheckingEncoder();
    }
    return instance;
  }

  @Override
  public Contextualized<KodkodProblem> encode(
      AbstractHistoryK encoding, Instance instance, ExecutionFormulaG formula) {
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

    Relation commitOrderAux = Relation.binary("CO's transitive reduction");
    Relation lastTxn = Relation.unary("last txn");
    // Relation commitOrder = Relation.binary("counterexample's commit order");

    TupleSet commitOrderLowerBound =
        ev.evaluate(encoding.initialTransaction()).product(ev.evaluate(encoding.normalTxns()));

    TupleSet commitOrderUpperBound =
        Util.irreflexiveBound(f, Util.unaryTupleSetToAtoms(ev.evaluate(encoding.normalTxns())));
    commitOrderUpperBound.addAll(commitOrderLowerBound);

    b.bound(commitOrderAux, commitOrderUpperBound);
    // b.bound(
    //    commitOrder,
    //    commitOrderLowerBound,
    //    commitOrderUpperBound); // TODO: Improve using info from so + wr
    b.bound(lastTxn, ev.evaluate(encoding.normalTxns()));

    Expression commitOrder = commitOrderAux.closure();

    Formula spec =
        Formula.and(
            commitOrderAux.totalOrder(transactions, initialTransaction, lastTxn),
            formula.apply(DirectAbstractHistoryEncoding.instance(), commitOrder));

    return new Contextualized<KodkodProblem>(
        DirectAbstractHistoryEncoding.instance(),
        Collections.singletonList(commitOrder),
        new KodkodProblem(spec, b));
  }
}
