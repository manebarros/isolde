package com.github.manebarros;

import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class AxiomaticDefinitions {

  public static Formula ReadAtomic(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(), h.wr(t1, x, t3), t3.in(t2.join(h.sessionOrder().union(h.binaryWr()))))
        .implies(t1.in(t2.join(e.commitOrder())))
        .forAll(
            x.oneOf(h.keys())
                .and(
                    t1.oneOf(h.txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(h.txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(h.txnThatReadAnyOf(x).difference(t1.union(t2)))))));
  }

  public static Formula ReadAtomicWoInitial(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(), h.wr(t1, x, t3), t3.in(t2.join(h.sessionOrder().union(h.binaryWr()))))
        .implies(t1.in(t2.join(e.commitOrder())))
        .forAll(
            x.oneOf(h.keys())
                .and(
                    t1.oneOf(h.txnThatWriteToAnyOf(x).intersection(h.normalTxns()))
                        .and(
                            t2.oneOf(h.txnThatWriteToAnyOf(x).intersection(h.normalTxns()))
                                .and(t3.oneOf(h.txnThatReadAnyOf(x).difference(t1.union(t2)))))));
  }

  public static Formula Causal(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(t1.eq(t2).not(), h.wr(t1, x, t3), h.causallyOrdered(t2, t3))
        .implies(t1.in(t2.join(e.commitOrder())))
        .forAll(
            x.oneOf(h.keys())
                .and(
                    t1.oneOf(h.txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(h.txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(h.txnThatReadAnyOf(x).difference(t1.union(t2)))))));
  }

  public static Formula Prefix(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(),
            h.wr(t1, x, t3),
            t2.product(t3)
                .in(e.commitOrder().reflexiveClosure().join(h.binaryWr().union(h.sessionOrder()))))
        .implies(t1.in(t2.join(e.commitOrder())))
        .forAll(
            x.oneOf(h.keys())
                .and(
                    t1.oneOf(h.txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(h.txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(h.txnThatReadAnyOf(x))))));
  }

  public static Formula Conflict(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable t4 = Variable.unary("t4");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");

    return Formula.and(
            t1.eq(t2).not(),
            h.wr(t1, x, t3),
            t2.product(t4).in(e.commitOrder().reflexiveClosure()),
            t4.product(t3).in(e.commitOrder()))
        .implies(t1.in(t2.join(e.commitOrder())))
        .forAll(
            x.oneOf(h.keys())
                .and(y.oneOf(h.keys()))
                .and(
                    t1.oneOf(h.txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(h.txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(h.txnThatWriteToAnyOf(y)))
                                .and(t4.oneOf(h.txnThatWriteToAnyOf(y))))));
  }

  public static Formula Serializability(AbstractHistoryK h, BiswasExecutionK e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(t1.eq(t2).not(), h.wr(t1, x, t3), t2.product(t3).in(e.commitOrder()))
        .implies(t1.in(t2.join(e.commitOrder())))
        .forAll(
            x.oneOf(h.keys())
                .and(
                    t1.oneOf(h.txnThatWriteToAnyOf(x))
                        .and(t2.oneOf(h.txnThatWriteToAnyOf(x)).and(t3.oneOf(h.transactions())))));
  }
}
