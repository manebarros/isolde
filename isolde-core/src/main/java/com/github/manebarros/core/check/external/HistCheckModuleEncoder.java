package com.github.manebarros.core.check.external;

import com.github.manebarros.core.AbstractHistoryK;
import com.github.manebarros.core.Execution;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.check.CheckingProblemExtender;
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
