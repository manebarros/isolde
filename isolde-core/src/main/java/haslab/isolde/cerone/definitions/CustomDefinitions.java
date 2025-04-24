package haslab.isolde.cerone.definitions;

import static haslab.isolde.biswas.definitions.HistoryOnlyIsolationCriterion.Causal;
import static haslab.isolde.biswas.definitions.IsolationCriterion.Prefix;
import static haslab.isolde.cerone.definitions.CeroneDefinitions.EXT;
import static haslab.isolde.cerone.definitions.CeroneDefinitions.SESSION;
import static haslab.isolde.cerone.definitions.CeroneDefinitions.TRANS_VIS;
import static haslab.isolde.kodkod.KodkodUtil.disjoint;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryExpression;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.kodkod.KodkodUtil;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class CustomDefinitions {
  public static ExecutionFormula<CeroneExecution> lostUpdate() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");
    return e ->
        e.history()
            .writes(t, x)
            .and(
                KodkodUtil.max(
                        e.ar(), e.ar().join(t).intersection(e.history().txnThatWriteToAnyOf(x)))
                    .in(e.vis().join(t))
                    .not())
            .forSome(t.oneOf(e.history().normalTxns()).and(x.oneOf(e.history().keys())));
  }

  public static HistoryFormula subtleLongFork() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    Variable i = Variable.unary("i");
    Variable j = Variable.unary("j");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");
    Variable xi = Variable.unary("xi");
    Variable yi = Variable.unary("yi");
    Variable xf = Variable.unary("xf");
    Variable yf = Variable.unary("yf");
    return h ->
        Formula.and(
                i.eq(j).not(),
                h.externalRead(i, x, xi),
                h.finalWrite(i, x, xf),
                h.externalRead(j, y, yi),
                h.finalWrite(j, y, yf),
                h.externalRead(t, x, xf),
                h.externalRead(t, y, yi),
                h.externalRead(s, x, xi),
                j.product(s).in(Causal.mandatoryCommitOrderEdges().resolve(h)))
            .forSome(
                t.oneOf(h.transactions())
                    .and(s.oneOf(h.transactions()))
                    .and(i.oneOf(h.transactions()))
                    .and(j.oneOf(h.transactions()))
                    .and(x.oneOf(h.keys()))
                    .and(y.oneOf(h.keys()))
                    .and(xi.oneOf(h.values()))
                    .and(xf.oneOf(h.values()))
                    .and(yi.oneOf(h.values()))
                    .and(yf.oneOf(h.values())));
  }

  public static HistoryFormula historyOnlyLostUpdate() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    Variable x = Variable.unary("x");
    Variable vi = Variable.unary("vi");
    Variable vt = Variable.unary("vt");
    Variable vs = Variable.unary("vs");
    return h ->
        Formula.and(
                t.eq(s).not(),
                h.externalRead(t, x, vi),
                h.externalRead(s, x, vi),
                h.finalWrite(t, x, vt),
                h.finalWrite(s, x, vs))
            .forSome(
                t.oneOf(h.transactions())
                    .and(s.oneOf(h.transactions()))
                    .and(x.oneOf(h.keys()))
                    .and(vi.oneOf(h.values()))
                    .and(vt.oneOf(h.values()))
                    .and(vs.oneOf(h.values())));
  }

  public static HistoryExpression versionOrders() {
    Variable x = Variable.unary("x");
    Variable v1 = Variable.unary("v1");
    Variable v2 = Variable.unary("v2");
    return h ->
        v1.product(v2)
            .in(versionOrder(x).resolve(h))
            .comprehension(x.oneOf(h.keys()).and(v1.oneOf(h.values()).and(v2.oneOf(h.values()))));
  }

  public static Expression versionOrder(BiswasExecution e, Expression x) {
    Variable m, n, t;
    m = Variable.unary("m");
    n = Variable.unary("n");
    t = Variable.unary("t");
    AbstractHistoryK h = e.history();
    return h.writerOf(x, m)
        .product(h.writerOf(x, n))
        .in(e.co())
        .comprehension(m.oneOf(h.valuesWrittenTo(x)).and(n.oneOf(h.valuesWrittenTo(x))));
  }

  public static HistoryExpression versionOrder(Expression x) {
    Variable m, n, t;
    m = Variable.unary("m");
    n = Variable.unary("n");
    t = Variable.unary("t");
    return h ->
        h.externalRead(t, x, m)
            .and(h.finalWrite(t, x, n))
            .forSome(t.oneOf(h.transactions()))
            .comprehension(m.oneOf(h.values()).and(n.oneOf(h.values())));
  }

  public static HistoryExpression knowsAtLeast() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");
    Variable v = Variable.unary("v");
    return h ->
        knowsAtLeast(t, x, v)
            .resolve(h)
            .comprehension(
                t.oneOf(h.transactions()).and(x.oneOf(h.keys())).and(v.oneOf(h.values())));
  }

  private static HistoryExpression knowsAtLeast(Expression t, Expression x) {
    return h -> {
      return x.join(
          KodkodUtil.maxPartialOrder(
                  h.causalOrder().closure(),
                  h.causalOrder().closure().join(t).intersection(h.txnThatWriteToAnyOf(x)))
              .join(h.finalWrites()));
    };
  }

  private static HistoryFormula knowsAtLeast(Expression t, Expression x, Expression v) {
    return h -> v.in(knowsAtLeast(t, x).resolve(h));
  }

  private static HistoryFormula knownVersion(Expression t, Expression x, Expression v) {
    return h -> {
      Variable s = Variable.unary("s");
      Variable u = Variable.unary("u");
      Variable m = Variable.unary("m");
      Expression previousThatWriteToX =
          h.writes(s, x).comprehension(s.oneOf(Causal.mandatoryCommitOrderEdges(h).join(t)));
      return Formula.or(
          h.finalWrite(t, x, v),
          h.writes(t, x).not().and(h.externalRead(t, x, v)),
          previousThatWriteToX.no().and(v.eq(h.initialValue(x))),
          h.writes(m, x)
              .not()
              .forAll(m.oneOf(u.join(Causal.mandatoryCommitOrderEdges(h))))
              .and(h.finalWrite(u, x, v))
              .forSome(u.oneOf(previousThatWriteToX)));
    };
  }

  private static Formula knowsVersionMoreRecentThan(
      BiswasExecution e, Expression t, Expression x, Expression v) {
    Variable n = Variable.unary("n");
    Variable a = Variable.unary("a");
    Formula readsMoreRecent =
        e.history()
            .externalRead(t, x, n)
            .and(isMoreRecentThan(e, n, v, x))
            .forSome(n.oneOf(e.history().values()));
    return readsMoreRecent.or(
        isMoreRecentThan(e, a, v, x).forSome(a.oneOf(knowsAtLeast(t, x).resolve(e.history()))));
  }

  private static HistoryFormula knowsVersionMoreRecentThan(
      Expression t, Expression x, Expression v) {
    return h -> {
      Variable n = Variable.unary("n");
      Variable a = Variable.unary("a");
      Formula readsMoreRecent =
          h.externalRead(t, x, n)
              .and(isMoreRecentThan(n, v, x).resolve(h))
              .forSome(n.oneOf(h.values()));
      return readsMoreRecent.or(
          isMoreRecentThan(a, v, x).resolve(h).forSome(a.oneOf(knowsAtLeast(t, x).resolve(h))));
    };
  }

  private static Formula isMoreRecentThan(
      BiswasExecution e, Expression v2, Expression v1, Expression x) {
    return v2.in(v1.join(versionOrder(e, x).closure()));
  }

  private static HistoryFormula isMoreRecentThan(Expression v2, Expression v1, Expression x) {
    return h -> v2.in(v1.join(versionOrder(x).resolve(h).closure()));
  }

  public static HistoryFormula newLongFork() {
    Variable s, t, x, y, xi, xf, yi, yf;
    s = Variable.unary("s");
    t = Variable.unary("t");
    x = Variable.unary("x");
    y = Variable.unary("y");
    xi = Variable.unary("xi");
    xf = Variable.unary("xf");
    yi = Variable.unary("yi");
    yf = Variable.unary("yf");

    return h ->
        Formula.and(
                x.eq(y).not(),
                h.externalRead(t, x, xi),
                h.externalRead(s, y, yi),
                knowsVersionMoreRecentThan(t, y, yi).resolve(h),
                knowsVersionMoreRecentThan(s, x, xi).resolve(h))
            .forSome(
                s.oneOf(h.transactions())
                    .and(t.oneOf(h.transactions()))
                    .and(
                        x.oneOf(h.keys())
                            .and(y.oneOf(h.keys()))
                            .and(xi.oneOf(h.values()))
                            .and(xf.oneOf(h.values()))
                            .and(yi.oneOf(h.values()))
                            .and(yf.oneOf(h.values()))));
  }

  public static Formula tripleFork(AbstractHistoryK h) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable t4 = Variable.unary("t4");
    Variable t5 = Variable.unary("t5");
    Variable t6 = Variable.unary("t6");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");
    Variable z = Variable.unary("z");
    return Formula.and(
            disjoint(x, y, z),
            disjoint(t1, t2, t3, t4, t5, t6),
            h.writes(t1, x),
            h.writes(t1, y),
            h.writes(t2, x),
            h.writes(t2, z),
            h.writes(t3, y),
            h.writes(t3, z),
            h.wr(t1, y, t4),
            h.wr(t2, z, t4),
            h.wr(t2, x, t5),
            h.wr(t3, y, t5),
            h.wr(t1, x, t6),
            h.wr(t3, z, t6))
        .forSome(
            t1.oneOf(h.transactions())
                .and(t2.oneOf(h.transactions()))
                .and(t3.oneOf(h.transactions()))
                .and(t4.oneOf(h.transactions()))
                .and(t5.oneOf(h.transactions()))
                .and(t6.oneOf(h.transactions()))
                .and(x.oneOf(h.keys()))
                .and(y.oneOf(h.keys()))
                .and(z.oneOf(h.keys())));
  }

  public static Formula newLongFork(BiswasExecution e) {
    Variable s, t, x, y, xi, xf, yi, yf;
    s = Variable.unary("s");
    t = Variable.unary("t");
    x = Variable.unary("x");
    y = Variable.unary("y");
    xi = Variable.unary("xi");
    yi = Variable.unary("yi");

    var h = e.history();
    return Formula.and(
            x.eq(y).not(),
            h.externalRead(t, x, xi),
            h.externalRead(s, y, yi),
            knowsVersionMoreRecentThan(e, t, y, yi),
            knowsVersionMoreRecentThan(e, s, x, xi))
        .forSome(
            s.oneOf(h.transactions())
                .and(t.oneOf(h.transactions()))
                .and(x.oneOf(h.keys()))
                .and(y.oneOf(h.keys()))
                .and(xi.oneOf(h.values()))
                .and(yi.oneOf(h.values())));
  }

  public static HistoryFormula genericLongFork() {
    Variable s, t, x, y, xi, xf, yi, yf;
    s = Variable.unary("s");
    t = Variable.unary("t");
    x = Variable.unary("x");
    y = Variable.unary("y");
    xi = Variable.unary("xi");
    xf = Variable.unary("xf");
    yi = Variable.unary("yi");
    yf = Variable.unary("yf");

    return h ->
        Formula.and(
                x.eq(y).not(),
                knownVersion(t, x, xi).resolve(h),
                knownVersion(t, x, yf).resolve(h),
                knownVersion(s, y, yi).resolve(h),
                knownVersion(s, y, xf).resolve(h),
                xf.in(xi.join(versionOrder(x).resolve(h).closure())),
                yf.in(yi.join(versionOrder(y).resolve(h).closure())))
            .forSome(
                s.oneOf(h.transactions())
                    .and(t.oneOf(h.transactions()))
                    .and(
                        x.oneOf(h.keys())
                            .and(y.oneOf(h.keys()))
                            .and(xi.oneOf(h.values()))
                            .and(xf.oneOf(h.values()))
                            .and(yi.oneOf(h.values()))
                            .and(yf.oneOf(h.values()))));
  }

  public static HistoryFormula historyOnlyLongFork() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    Variable i = Variable.unary("i");
    Variable j = Variable.unary("j");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");
    Variable xi = Variable.unary("xi");
    Variable yi = Variable.unary("yi");
    Variable xf = Variable.unary("xf");
    Variable yf = Variable.unary("yf");
    return h ->
        Formula.and(
                i.eq(j).not(),
                h.externalRead(i, x, xi),
                h.finalWrite(i, x, xf),
                h.externalRead(j, y, yi),
                h.finalWrite(j, y, yf),
                h.externalRead(t, x, xf),
                h.externalRead(t, y, yi),
                h.externalRead(s, x, xi),
                h.externalRead(s, y, yf))
            .forSome(
                t.oneOf(h.transactions())
                    .and(s.oneOf(h.transactions()))
                    .and(i.oneOf(h.transactions()))
                    .and(j.oneOf(h.transactions()))
                    .and(x.oneOf(h.keys()))
                    .and(y.oneOf(h.keys()))
                    .and(xi.oneOf(h.values()))
                    .and(xf.oneOf(h.values()))
                    .and(yi.oneOf(h.values()))
                    .and(yf.oneOf(h.values())));
  }

  public static HistoryFormula prefixViolation() {
    Variable s = Variable.unary("s");
    Variable t = Variable.unary("t");
    Variable u = Variable.unary("u");
    Variable v = Variable.unary("v");
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");
    return h ->
        Formula.and(
                v.product(s).in(Causal.mandatoryCommitOrderEdges().resolve(h)),
                s.product(t).in(Causal.mandatoryCommitOrderEdges().resolve(h)),
                h.wr(t, x, u),
                h.writes(s, y),
                h.wr(v, y, u))
            .forSome(
                s.oneOf(h.transactions())
                    .and(t.oneOf(h.transactions()))
                    .and(u.oneOf(h.transactions()))
                    .and(v.oneOf(h.transactions()))
                    .and(x.oneOf(h.keys()))
                    .and(y.oneOf(h.keys())));
  }

  public static ExecutionFormula<CeroneExecution> customUA =
      EXT.and(SESSION).and(lostUpdate().not());

  public static ExecutionFormula<CeroneExecution> customPSI =
      EXT.and(SESSION).and(TRANS_VIS).and(historyOnlyLostUpdate().not().asExecutionFormula());

  public static HistoryFormula customSI = newCustomPC().and(historyOnlyLostUpdate().not());

  public static HistoryFormula customPC =
      Causal.historyOnlySpec()
          .and(genericLongFork().not())
          // .and(subtleLongFork().not())
          .and(prefixViolation().not());

  private static Expression otherPcMandatoryCommitOrderEdges(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable t4 = Variable.unary("t4");
    Variable x = Variable.unary("x");
    return Formula.or(
            // e.history().causallyOrdered(t3, t2),
            t3.product(t2).in(e.co()),
            Formula.and(
                    e.history().writes(t2, x),
                    e.history().causallyOrdered(t3, t4),
                    t1.product(t2).in(e.co()),
                    e.history().wr(t1, x, t4))
                .forSome(
                    x.oneOf(e.history().keys())
                        .and(t1.oneOf(e.history().transactions()))
                        .and(t4.oneOf(e.history().transactions()))))
        .comprehension(
            t3.oneOf(e.history().transactions()).and(t2.oneOf(e.history().transactions())))
        .closure();
  }

  public static HistoryExpression mandatoryCommitOrderEdgesPrefix(int depth) {
    return h -> {
      Expression mandatoryEdges = Causal.mandatoryCommitOrderEdges(h);
      for (int i = 0; i < depth; i++) {
        mandatoryEdges =
            Prefix.mandatoryCommitOrderEdges(
                new BiswasExecution(
                    h, otherPcMandatoryCommitOrderEdges(new BiswasExecution(h, mandatoryEdges))));
      }
      return mandatoryEdges;
    };
  }

  public static HistoryFormula newCustomPC() {
    return h -> {
      // Expression mandatoryCommitOrderEdgesPrefix =
      //    Prefix.mandatoryCommitOrderEdges(
      //        new BiswasExecution(
      //            h,
      //            Prefix.mandatoryCommitOrderEdges(
      //                new BiswasExecution(
      //                    h,
      //                    otherPcMandatoryCommitOrderEdges(
      //                        new BiswasExecution(h, Causal.mandatoryCommitOrderEdges(h)))))));
      Expression mandatoryCommitOrderEdges = mandatoryCommitOrderEdgesPrefix(3).resolve(h);

      return KodkodUtil.acyclic(mandatoryCommitOrderEdges)
          .and(newLongFork(new BiswasExecution(h, mandatoryCommitOrderEdges)).not())
          .and(tripleFork(h).not());
    };
  }
}
