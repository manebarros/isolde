package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistorySchema;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface ExecutionModule<
    E extends Execution, I extends AtomsContainer, S, C extends AtomsContainer> {
  List<E> executions(HistorySchema history);

  int executions();

  C createContext(I input);

  Formula encode(
      Bounds bounds,
      List<ExecutionFormula<E>> formulas,
      C context,
      S spec,
      AbstractHistoryRel history);
}
