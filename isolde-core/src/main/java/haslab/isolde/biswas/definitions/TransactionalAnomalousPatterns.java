package haslab.isolde.biswas.definitions;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryExpression;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.kodkod.KodkodUtil;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class TransactionalAnomalousPatterns {

  public static final ExecutionFormula<BiswasExecution> k = TransactionalAnomalousPatterns::k;
  public static final ExecutionFormula<BiswasExecution> l = TransactionalAnomalousPatterns::l;
  public static final ExecutionFormula<BiswasExecution> m = TransactionalAnomalousPatterns::m;
  public static final ExecutionFormula<BiswasExecution> n = TransactionalAnomalousPatterns::n;

  public static final ExecutionFormula<BiswasExecution> ReadAtomic = k.not().and(l.not());
  public static final ExecutionFormula<BiswasExecution> Causal =
      k.not().and(l.not()).and(m.not()).and(n.not());

  public static Formula k(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");

    return Formula.and(
            x.eq(y).not(),
            t1.eq(t2).not(),
            e.history().wr(t1, x, t3),
            e.history().wr(t2, y, t3),
            e.history().causallyOrdered(t1, t2))
        .forSome(
            x.oneOf(e.history().keys())
                .and(y.oneOf(e.history().keys()))
                .and(t1.oneOf(e.history().txnThatWriteToAnyOf(x)))
                .and(t2.oneOf(e.history().txnThatWriteToAnyOf(x)))
                .and(t3.oneOf(e.history().txnThatReadAnyOf(x))));
  }

  public static Formula l(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");

    return Formula.and(
            x.eq(y).not(),
            t1.eq(t2).not(),
            e.history().wr(t2, y, t3),
            e.history().wr(t1, x, t3),
            t1.product(t2).in(e.co()))
        .forSome(
            x.oneOf(e.history().keys())
                .and(y.oneOf(e.history().keys()))
                .and(t1.oneOf(e.history().txnThatWriteToAnyOf(x)))
                .and(t2.oneOf(e.history().txnThatWriteToAnyOf(x)))
                .and(t3.oneOf(e.history().txnThatReadAnyOf(x))));
  }

  public static Formula m(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(),
            e.history().wr(t1, x, t3),
            e.history().causallyOrdered(t1, t2),
            e.history().causallyOrdered(t2, t3))
        .forSome(
            x.oneOf(e.history().keys())
                .and(t1.oneOf(e.history().txnThatWriteToAnyOf(x)))
                .and(t2.oneOf(e.history().txnThatWriteToAnyOf(x)))
                .and(t3.oneOf(e.history().txnThatReadAnyOf(x).difference(t1.union(t2)))));
  }

  public static Formula n(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(),
            e.history().wr(t1, x, t3),
            t2.in(t1.join(e.co())),
            e.history().causallyOrdered(t2, t3))
        .forSome(
            x.oneOf(e.history().keys())
                .and(t1.oneOf(e.history().txnThatWriteToAnyOf(x)))
                .and(t2.oneOf(e.history().txnThatWriteToAnyOf(x)))
                .and(t3.oneOf(e.history().txnThatReadAnyOf(x).difference(t1.union(t2)))));
  }

  public static HistoryExpression mandatoryCommitOrderEdgesCC() {
    return h -> {
      Variable t1 = Variable.unary("t1");
      Variable t2 = Variable.unary("t2");
      Variable t3 = Variable.unary("t3");
      Variable x = Variable.unary("x");

      return h.causallyOrdered(t2, t1)
          .or(
              Formula.and(
                      t1.eq(t2).not(), h.writes(t2, x), h.wr(t1, x, t3), h.causallyOrdered(t2, t3))
                  .forSome(
                      x.oneOf(h.keys()).and(t3.oneOf(h.transactions().difference(t1.union(t2))))))
          .comprehension(t2.oneOf(h.transactions()).and(t1.oneOf(h.transactions())))
          .closure();
    };
  }

  public static HistoryExpression mandatoryCommitOrderEdgesRA() {
    return h -> {
      Variable t1 = Variable.unary("t1");
      Variable t2 = Variable.unary("t2");
      Variable t3 = Variable.unary("t3");
      Variable x = Variable.unary("x");

      return h.causallyOrdered(t2, t1)
          .or(
              Formula.and(
                      t1.eq(t2).not(),
                      h.writes(t2, x),
                      h.wr(t1, x, t3),
                      t2.product(t3).in(h.sessionOrder().union(h.binaryWr())))
                  .forSome(
                      x.oneOf(h.keys()).and(t3.oneOf(h.transactions().difference(t1.union(t2))))))
          .comprehension(t2.oneOf(h.transactions()).and(t1.oneOf(h.transactions())))
          .closure();
    };
  }

  public static HistoryFormula Causal() {
    return h -> n(new BiswasExecution(h, h.mandatoryCommitOrderEdgesCC())).not();
  }

  public static HistoryFormula CausalSimpler() {
    return h -> KodkodUtil.acyclic(h.mandatoryCommitOrderEdgesCC());
  }

  public static HistoryFormula HistBasedRa() {
    return h -> KodkodUtil.acyclic(h.mandatoryCommitOrderEdgesRA());
  }

  public static ExecutionFormula<BiswasExecution> raAnomaly() {
    return e -> {
      Variable t1 = Variable.unary("t1");
      Variable t2 = Variable.unary("t2");
      Variable t3 = Variable.unary("t3");
      Variable x = Variable.unary("x");

      return Formula.and(
              t1.eq(t2).not(),
              e.history().wr(t1, x, t3),
              t2.in(t1.join(e.co())),
              t3.in(t2.join(e.history().sessionOrder().union(e.history().binaryWr()))))
          .forSome(
              x.oneOf(e.history().keys())
                  .and(t1.oneOf(e.history().txnThatWriteToAnyOf(x)))
                  .and(t2.oneOf(e.history().txnThatWriteToAnyOf(x)))
                  .and(t3.oneOf(e.history().txnThatReadAnyOf(x).difference(t1.union(t2)))));
    };
  }
}
