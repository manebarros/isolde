package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import java.util.List;

public interface ExecutionConstraintsEncoder<I extends Input, T, E extends Execution> {
  List<E> executions(AbstractHistoryK historyEncoding);

  ProblemExtender<T> encode(
      I input, AbstractHistoryK historyEncoding, List<ExecutionFormula<E>> formulas);

  default ProblemExtender<T> encode(
      HistoryConstraintProblem<I, ?, T> problem, List<ExecutionFormula<E>> formulas) {
    return encode(problem.getInput(), problem.getHistEncoder().encoding(), formulas);
  }
}
