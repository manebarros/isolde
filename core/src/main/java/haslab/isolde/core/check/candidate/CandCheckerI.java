package haslab.isolde.core.check.candidate;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.kodkod.KodkodProblem;
import kodkod.engine.KodkodSolver;
import kodkod.engine.Solution;
import kodkod.instance.Instance;

public interface CandCheckerI<E extends Execution> {
  E execution();

  KodkodProblem encode(Instance instance, AbstractHistoryK context, ExecutionFormula<E> formula);

  default Solution check(
      Instance instance,
      AbstractHistoryK context,
      ExecutionFormula<E> formula,
      KodkodSolver solver) {
    return this.encode(instance, context, formula).solve(solver);
  }
}
