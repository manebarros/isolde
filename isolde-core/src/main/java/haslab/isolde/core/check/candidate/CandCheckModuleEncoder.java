package haslab.isolde.core.check.candidate;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.check.CheckingProblemExtender;
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
