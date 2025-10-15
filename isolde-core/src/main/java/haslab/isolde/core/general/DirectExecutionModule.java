package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface DirectExecutionModule<
        E extends Execution, I extends AtomsContainer, C extends AtomsContainer>
    extends ExecutionModule<E, I, Void, C> {

  Formula encode(
      Bounds bounds, List<ExecutionFormula<E>> formulas, C context, AbstractHistoryRel history);

  @Override
  default Formula encode(
      Bounds bounds,
      List<ExecutionFormula<E>> formulas,
      C context,
      Void spec,
      AbstractHistoryRel history) {
    return encode(bounds, formulas, context, history);
  }
}
