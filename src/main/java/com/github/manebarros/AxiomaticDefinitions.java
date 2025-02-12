package com.github.manebarros;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class AxiomaticDefinitions {

  public static Formula ReadAtomic(AbstractHistoryK h, Expression commitOrder) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(), h.wr(t1, x, t3), t3.in(t2.join(h.sessionOrder().union(h.binaryWr()))))
        .implies(t1.in(t2.join(commitOrder)))
        .forAll(
            x.oneOf(h.keys())
                .and(
                    t1.oneOf(h.txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(h.txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(h.txnThatReadAnyOf(x).difference(t1.union(t2)))))));
  }

  public static Formula Causal(AbstractHistoryK h, Expression commitOrder) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(t1.eq(t2).not(), h.wr(t1, x, t3), h.causallyOrdered(t2, t3))
        .implies(t1.in(t2.join(commitOrder)))
        .forAll(
            x.oneOf(h.keys())
                .and(
                    t1.oneOf(h.txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(h.txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(h.txnThatReadAnyOf(x).difference(t1.union(t2)))))));
  }
}
