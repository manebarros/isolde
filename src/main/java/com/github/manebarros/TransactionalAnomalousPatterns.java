package com.github.manebarros;

import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class TransactionalAnomalousPatterns {
  public static Formula k(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");

    return Formula.and(
            x.eq(y).not(),
            t1.eq(t2).not(),
            h.wr(t1, x, t3),
            h.wr(t2, y, t3),
            h.causallyOrdered(t1, t2))
        .forSome(
            x.oneOf(h.keys())
                .and(y.oneOf(h.keys()))
                .and(t1.oneOf(h.txnThatWriteToAnyOf(x)))
                .and(t2.oneOf(h.txnThatWriteToAnyOf(x)))
                .and(t3.oneOf(h.txnThatReadAnyOf(x))));
  }

  public static Formula l(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");

    return Formula.and(
            x.eq(y).not(),
            t1.eq(t2).not(),
            h.wr(t2, y, t3),
            h.wr(t1, x, t3),
            t1.product(t2).in(e.commitOrder()))
        .forSome(
            x.oneOf(h.keys())
                .and(y.oneOf(h.keys()))
                .and(t1.oneOf(h.txnThatWriteToAnyOf(x)))
                .and(t2.oneOf(h.txnThatWriteToAnyOf(x)))
                .and(t3.oneOf(h.txnThatReadAnyOf(x))));
  }

  public static Formula m(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(), h.wr(t1, x, t3), h.causallyOrdered(t1, t2), h.causallyOrdered(t2, t3))
        .forSome(
            x.oneOf(h.keys())
                .and(t1.oneOf(h.txnThatWriteToAnyOf(x)))
                .and(t2.oneOf(h.txnThatWriteToAnyOf(x)))
                .and(t3.oneOf(h.txnThatReadAnyOf(x).difference(t1.union(t2)))));
  }

  public static Formula n(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(),
            h.wr(t1, x, t3),
            t2.in(t1.join(e.commitOrder())),
            h.causallyOrdered(t2, t3))
        .forSome(
            x.oneOf(h.keys())
                .and(t1.oneOf(h.txnThatWriteToAnyOf(x)))
                .and(t2.oneOf(h.txnThatWriteToAnyOf(x)))
                .and(t3.oneOf(h.txnThatReadAnyOf(x).difference(t1.union(t2)))));
  }
}
