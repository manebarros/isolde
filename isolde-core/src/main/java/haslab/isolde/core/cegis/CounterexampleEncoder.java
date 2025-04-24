package haslab.isolde.core.cegis;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public interface CounterexampleEncoder<E extends Execution> {
  HistoryFormula guide(Instance instance, E execution, ExecutionFormula<E> formula, Bounds bounds);
}
