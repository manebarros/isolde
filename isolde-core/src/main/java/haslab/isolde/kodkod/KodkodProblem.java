package haslab.isolde.kodkod;

import kodkod.ast.Formula;
import kodkod.engine.KodkodSolver;
import kodkod.engine.Solution;
import kodkod.instance.Bounds;

public record KodkodProblem(Formula formula, Bounds bounds) {
  public Solution solve(KodkodSolver solver) {
    return solver.solve(formula, bounds);
  }
}
