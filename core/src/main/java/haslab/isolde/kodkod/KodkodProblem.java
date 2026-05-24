package haslab.isolde.kodkod;

import kodkod.ast.Formula;
import kodkod.engine.KodkodSolver;
import kodkod.engine.Solution;
import kodkod.instance.Bounds;

public record KodkodProblem(Formula formula, Bounds bounds) {
  public Solution solve(KodkodSolver solver) {
    return solver.solve(formula, bounds);
  }

  public KodkodProblem clone() {
    return new KodkodProblem(this.formula, this.bounds.clone());
  }

  public KodkodProblem and(Formula formula) {
    return new KodkodProblem(this.formula.and(formula), this.bounds);
  }

  public KodkodProblem and(Formula formula, Bounds newBounds) {
    return new KodkodProblem(this.formula.and(formula), newBounds);
  }
}
