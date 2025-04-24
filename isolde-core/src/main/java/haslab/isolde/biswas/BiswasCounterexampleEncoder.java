package haslab.isolde.biswas;

import haslab.isolde.core.cegis.CounterexampleEncoder;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

public class BiswasCounterexampleEncoder implements CounterexampleEncoder<BiswasExecution> {

  @Override
  public HistoryFormula guide(
      Instance instance,
      BiswasExecution execution,
      ExecutionFormula<BiswasExecution> formula,
      Bounds bounds) {
    var eval = new Evaluator(instance);
    TupleSet commitOrderVal = eval.evaluate(execution.co());
    Relation cexCommitOrderRel = Relation.binary("cexCommitOrder");
    bounds.boundExactly(cexCommitOrderRel, commitOrderVal);
    return h -> formula.resolve(new BiswasExecution(h, cexCommitOrderRel));
  }
}
