package haslab.isolde.core.check.external;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.history.History;
import haslab.isolde.kodkod.KodkodProblem;
import kodkod.engine.KodkodSolver;
import kodkod.engine.Solution;

public interface HistCheckerI<E extends Execution> {
  E execution();

  KodkodProblem encode(History history, ExecutionFormula<E> formula);

  default Solution check(History history, ExecutionFormula<E> formula, KodkodSolver solver) {
    return this.encode(history, formula).solve(solver);
  }
}
