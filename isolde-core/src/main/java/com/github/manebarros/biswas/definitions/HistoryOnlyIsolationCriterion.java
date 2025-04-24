package com.github.manebarros.biswas.definitions;

import com.github.manebarros.biswas.BiswasExecution;
import com.github.manebarros.core.AbstractHistoryK;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.HistoryExpression;
import com.github.manebarros.core.HistoryFormula;
import com.github.manebarros.kodkod.KodkodUtil;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

@FunctionalInterface
public interface HistoryOnlyIsolationCriterion extends IsolationCriterion {

  HistoryFormula historyOnlyCommitEdgeCriteria(
      Expression t1, Expression t2, Expression t3, Expression x);

  @Override
  default ExecutionFormula<BiswasExecution> commitEdgeCriteria(
      Expression t1, Expression t2, Expression t3, Expression x) {
    return e -> historyOnlyCommitEdgeCriteria(t1, t2, t3, x).resolve(e.history());
  }

  default HistoryOnlyIsolationCriterion and(HistoryOnlyIsolationCriterion criterion) {
    return (t1, t2, t3, x) ->
        criterion
            .historyOnlyCommitEdgeCriteria(t1, t2, t3, x)
            .and(historyOnlyCommitEdgeCriteria(t1, t2, t3, x));
  }

  default Expression mandatoryCommitOrderEdges(AbstractHistoryK h) {
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
                    historyOnlyCommitEdgeCriteria(t1, t2, t3, x).resolve(h))
                .forSome(x.oneOf(h.keys()).and(t3.oneOf(h.transactions()))))
        .comprehension(t2.oneOf(h.transactions()).and(t1.oneOf(h.transactions())))
        .closure();
  }

  default HistoryExpression mandatoryCommitOrderEdges() {
    return h -> this.mandatoryCommitOrderEdges(h);
  }

  default HistoryFormula historyOnlySpec() {
    return h -> KodkodUtil.acyclic(mandatoryCommitOrderEdges().resolve(h));
  }

  public static HistoryOnlyIsolationCriterion ReadAtomic =
      (t1, t2, t3, x) -> h -> t3.in(t2.join(h.sessionOrder().union(h.binaryWr())));

  public static HistoryOnlyIsolationCriterion Causal =
      (t1, t2, t3, x) -> h -> h.causallyOrdered(t2, t3);
}
