package com.github.manebarros.core.check.candidate;

import com.github.manebarros.core.AbstractHistoryK;
import com.github.manebarros.core.Execution;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.check.CheckingProblemExtender;
import java.util.List;
import kodkod.instance.Instance;

public interface CandCheckModuleEncoder<E extends Execution> {
  List<E> executions(AbstractHistoryK historyEncoding);

  CheckingProblemExtender encode(
      Instance instance,
      AbstractHistoryK context,
      AbstractHistoryK historyEncoding,
      List<ExecutionFormula<E>> formulas);
}
