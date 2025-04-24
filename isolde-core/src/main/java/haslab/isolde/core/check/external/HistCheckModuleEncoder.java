package haslab.isolde.core.check.external;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.check.CheckingProblemExtender;
import java.util.List;

public interface HistCheckModuleEncoder<E extends Execution> {
  List<E> executions(AbstractHistoryK historyEncoding);

  CheckingProblemExtender encode(
      CheckingIntermediateRepresentation intermediateRepresentation,
      AbstractHistoryK historyEncoding,
      List<ExecutionFormula<E>> formulas);

  default CheckingProblemExtender encode(
      HistCheckProblem problem, List<ExecutionFormula<E>> formulas) {
    return encode(
        problem.getIntermediateRepresentation(),
        problem.getHistoryCheckingEncoder().encoding(),
        formulas);
  }
}
