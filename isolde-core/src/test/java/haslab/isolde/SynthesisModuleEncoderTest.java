package haslab.isolde;

import static org.junit.jupiter.api.Assertions.assertTrue;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.general.ExecutionModule;
import haslab.isolde.core.synth.DefaultHistorySynthesisEncoder;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.FolSynthesisProblem;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.core.synth.TransactionTotalOrderInfo;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import org.junit.jupiter.api.Test;

public interface SynthesisModuleEncoderTest<E extends Execution> {
  ExecutionModule<FolSynthesisInput, TransactionTotalOrderInfo, E> encoder();

  default Scope scope() {
    return new Scope(6, 6, 6, 6);
  }

  default void assertSat(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem =
        new FolSynthesisProblem(scope(), new DefaultHistorySynthesisEncoder());
    problem.register(encoder(), formula);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertSat(List<ExecutionFormula<E>> formulas) {
    FolSynthesisProblem problem =
        new FolSynthesisProblem(scope(), new DefaultHistorySynthesisEncoder());
    problem.register(encoder(), formulas);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertUnsat(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem =
        new FolSynthesisProblem(scope(), new DefaultHistorySynthesisEncoder());
    problem.register(encoder(), formula);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertFact(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem =
        new FolSynthesisProblem(scope(), new DefaultHistorySynthesisEncoder());
    problem.register(encoder(), formula.not());
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertSatWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem =
        FolSynthesisProblem.withNoTotalOrder(scope(), new DefaultHistorySynthesisEncoder());
    problem.register(encoder(), formula);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertUnsatWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem =
        FolSynthesisProblem.withNoTotalOrder(scope(), new DefaultHistorySynthesisEncoder());
    problem.register(encoder(), formula);
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertFactWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisProblem problem =
        FolSynthesisProblem.withNoTotalOrder(scope(), new DefaultHistorySynthesisEncoder());
    problem.register(encoder(), formula.not());
    Solution sol = problem.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  @Test
  default void itIsImpossibleToSynthesizeTriviallyUnsatFormula() {
    assertUnsat(e -> Formula.FALSE);
  }
}
