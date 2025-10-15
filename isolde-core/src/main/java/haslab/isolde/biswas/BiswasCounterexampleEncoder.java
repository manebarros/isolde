package haslab.isolde.biswas;

import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.CounterexampleEncoder;
import haslab.isolde.kodkod.Util;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

public class BiswasCounterexampleEncoder implements CounterexampleEncoder<BiswasExecution> {

  private static BiswasCounterexampleEncoder instance = null;

  private BiswasCounterexampleEncoder() {}

  public static BiswasCounterexampleEncoder instance() {
    if (instance != null) {
      instance = new BiswasCounterexampleEncoder();
    }
    return instance;
  }

  @Override
  public HistoryFormula guide(
      Instance instance,
      BiswasExecution execution,
      ExecutionFormula<BiswasExecution> formula,
      Bounds bounds) {
    var eval = new Evaluator(instance);
    TupleSet commitOrderVal =
        Util.convert(eval, execution, BiswasExecution::co, bounds.universe().factory(), 2);
    Relation cexCommitOrderRel = Relation.binary("cexCommitOrder");
    bounds.boundExactly(cexCommitOrderRel, commitOrderVal);
    return h -> formula.resolve(new BiswasExecution(h, cexCommitOrderRel));
  }
}
