package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryRel;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

@FunctionalInterface
public interface ProblemExtendingStrategy<T, S> {

  Formula extend(
      Formula formula,
      Bounds bounds,
      T helper,
      AbstractHistoryRel history,
      List<? extends ExecutionModuleInstance<?, ?, S, ?>> extenders);
}
