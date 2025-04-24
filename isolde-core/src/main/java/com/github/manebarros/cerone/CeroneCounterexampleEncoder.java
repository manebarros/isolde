package com.github.manebarros.cerone;

import com.github.manebarros.core.cegis.CounterexampleEncoder;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.HistoryFormula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

public class CeroneCounterexampleEncoder implements CounterexampleEncoder<CeroneExecution> {

  @Override
  public HistoryFormula guide(
      Instance instance,
      CeroneExecution execution,
      ExecutionFormula<CeroneExecution> formula,
      Bounds bounds) {
    var eval = new Evaluator(instance);
    TupleSet visVal = eval.evaluate(execution.vis());
    TupleSet arVal = eval.evaluate(execution.ar());
    Relation cexVisRel = Relation.binary("cex vis");
    Relation cexArRel = Relation.binary("cex ar");
    bounds.boundExactly(cexVisRel, visVal);
    bounds.boundExactly(cexArRel, arVal);
    return h -> formula.resolve(new CeroneExecution(h, cexVisRel, cexArRel));
  }
}
