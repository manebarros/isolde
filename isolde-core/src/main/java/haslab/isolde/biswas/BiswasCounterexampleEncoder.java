package haslab.isolde.biswas;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.CounterexampleEncoder;
import haslab.isolde.kodkod.Util;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

public class BiswasCounterexampleEncoder implements CounterexampleEncoder<BiswasExecution> {

  private static BiswasCounterexampleEncoder instance = null;

  private BiswasCounterexampleEncoder() {}

  public static BiswasCounterexampleEncoder instance() {
    if (instance == null) {
      instance = new BiswasCounterexampleEncoder();
    }
    return instance;
  }

  private Formula wellFormed(BiswasExecution execution) {
    AbstractHistoryK h = execution.history();
    return h.sessionOrder().union(h.binaryWr()).in(execution.co());
  }

  @Override
  public HistoryFormula guide(
      Instance instance,
      BiswasExecution execution,
      ExecutionFormula<BiswasExecution> formula,
      Bounds bounds) {
    ExecutionFormula<BiswasExecution> feedbackFormula =
        e ->
            wellFormed(e)
                .implies(
                    formula.resolve(
                        e)); // We should only enforce the formula on well-formed executions!
    var eval = new Evaluator(instance);
    TupleSet commitOrderVal =
        Util.convert(eval, execution, BiswasExecution::co, bounds.universe().factory(), 2);
    Relation cexCommitOrderRel = Relation.binary("cexCommitOrder");
    bounds.boundExactly(cexCommitOrderRel, commitOrderVal);
    return h -> feedbackFormula.resolve(new BiswasExecution(h, cexCommitOrderRel));
  }
}
