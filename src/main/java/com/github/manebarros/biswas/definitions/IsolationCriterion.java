package com.github.manebarros.biswas.definitions;

import com.github.manebarros.biswas.BiswasExecution;
import com.github.manebarros.core.ExecutionFormula;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

@FunctionalInterface
public interface IsolationCriterion {

  ExecutionFormula<BiswasExecution> commitEdgeCriteria(
      Expression t1, Expression t2, Expression t3, Expression x);

  default IsolationCriterion and(IsolationCriterion criterion) {
    return (t1, t2, t3, x) ->
        criterion.commitEdgeCriteria(t1, t2, t3, x).and(this.commitEdgeCriteria(t1, t2, t3, x));
  }

  default ExecutionFormula<BiswasExecution> spec() {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return e ->
        Formula.and(
                t1.eq(t2).not(),
                e.history().wr(t1, x, t3),
                commitEdgeCriteria(t1, t2, t3, x).resolve(e))
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
