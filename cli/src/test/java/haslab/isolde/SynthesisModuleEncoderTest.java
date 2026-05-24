package haslab.isolde;

import static org.junit.jupiter.api.Assertions.assertTrue;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.FolSynthesisProblem;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.core.synth.Scope;
import java.util.List;
import java.util.Optional;
import kodkod.ast.Formula;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.instance.TupleSet;
import org.junit.jupiter.api.Test;

public interface SynthesisModuleEncoderTest<E extends Execution> {
  ExecutionModuleConstructor<E, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
      constructor();

  default Scope scope() {
    return new Scope(6);
  }

  // Test with total order
  default void assertSat(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem = FolSynthesisProblem.withTotalOrder(scope());
    problem.register(constructor(), formula);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertSat(List<ExecutionFormula<E>> formulas) {
    FolSynthesisProblem problem = FolSynthesisProblem.withTotalOrder(scope());
    problem.register(constructor(), formulas);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertUnsat(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem = FolSynthesisProblem.withTotalOrder(scope());
    problem.register(constructor(), formula);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertFact(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem = FolSynthesisProblem.withTotalOrder(scope());
    problem.register(constructor(), formula.not());
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  // Tests without total order
  default void assertSatWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem = FolSynthesisProblem.withoutTotalOrder(scope());
    problem.register(constructor(), formula);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertUnsatWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem = FolSynthesisProblem.withoutTotalOrder(scope());
    problem.register(constructor(), formula);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertFactWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem = FolSynthesisProblem.withoutTotalOrder(scope());
    problem.register(constructor(), formula.not());
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  @Test
  default void itIsImpossibleToSynthesizeTriviallyUnsatFormula() {
    assertUnsat(e -> Formula.FALSE);
  }
}
