package haslab.isolde.biswas.definitions;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.core.ExecutionFormula;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class AxiomaticDefinitions {

  public static ExecutionFormula<BiswasExecution> ReadAtomic = AxiomaticDefinitions::ReadAtomic;
  public static ExecutionFormula<BiswasExecution> Causal = AxiomaticDefinitions::Causal;
  public static ExecutionFormula<BiswasExecution> Prefix = AxiomaticDefinitions::Prefix;
  public static ExecutionFormula<BiswasExecution> Snapshot = AxiomaticDefinitions::Snapshot;
  public static ExecutionFormula<BiswasExecution> Ser = AxiomaticDefinitions::Serializability;

  public static Formula ReadAtomic(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(),
            e.history().wr(t1, x, t3),
            t3.in(t2.join(e.history().sessionOrder().union(e.history().binaryWr()))))
        .implies(t1.in(t2.join(e.co())))
        .forAll(
            x.oneOf(e.history().keys())
                .and(
                    t1.oneOf(e.history().txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(e.history().txnThatWriteToAnyOf(x))
                                .and(
                                    t3.oneOf(
                                        e.history()
                                            .txnThatReadAnyOf(x)
                                            .difference(t1.union(t2)))))));
  }

  public static Formula Causal(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(), e.history().wr(t1, x, t3), e.history().causallyOrdered(t2, t3))
        .implies(t1.in(t2.join(e.co())))
        .forAll(
            x.oneOf(e.history().keys())
                .and(
                    t1.oneOf(e.history().txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(e.history().txnThatWriteToAnyOf(x))
                                .and(
                                    t3.oneOf(
                                        e.history()
                                            .txnThatReadAnyOf(x)
                                            .difference(t1.union(t2)))))));
  }

  public static Formula Prefix(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(),
            e.history().wr(t1, x, t3),
            t2.product(t3)
                .in(
                    e.co()
                        .reflexiveClosure()
                        .join(e.history().binaryWr().union(e.history().sessionOrder()))))
        .implies(t1.in(t2.join(e.co())))
        .forAll(
            x.oneOf(e.history().keys())
                .and(
                    t1.oneOf(e.history().txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(e.history().txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(e.history().txnThatReadAnyOf(x))))));
  }

  public static Formula Conflict(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable t4 = Variable.unary("t4");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");

    return Formula.and(
            t1.eq(t2).not(),
            e.history().wr(t1, x, t3),
            t2.product(t4).in(e.co().reflexiveClosure()),
            t4.product(t3).in(e.co()))
        .implies(t1.in(t2.join(e.co())))
        .forAll(
            x.oneOf(e.history().keys())
                .and(y.oneOf(e.history().keys()))
                .and(
                    t1.oneOf(e.history().txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(e.history().txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(e.history().txnThatWriteToAnyOf(y)))
                                .and(t4.oneOf(e.history().txnThatWriteToAnyOf(y))))));
  }

  public static Formula Snapshot(BiswasExecution e) {
    return Prefix(e).and(Conflict(e));
  }

  public static Formula Serializability(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(t1.eq(t2).not(), e.history().wr(t1, x, t3), t2.product(t3).in(e.co()))
        .implies(t1.in(t2.join(e.co())))
        .forAll(
            x.oneOf(e.history().keys())
                .and(
                    t1.oneOf(e.history().txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(e.history().txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(e.history().transactions())))));
  }
}
