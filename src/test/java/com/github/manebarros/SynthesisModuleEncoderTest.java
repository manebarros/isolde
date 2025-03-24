package com.github.manebarros;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.manebarros.core.DefaultHistorySynthesisEncoder;
import com.github.manebarros.core.Execution;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.FolSynthesisEncoder;
import com.github.manebarros.core.Scope;
import com.github.manebarros.core.SynthesisModule;
import com.github.manebarros.core.SynthesisModuleEncoder;
import java.util.List;
import kodkod.engine.Solution;
import kodkod.engine.Solver;

public interface SynthesisModuleEncoderTest<E extends Execution> {
  SynthesisModuleEncoder<E> encoder();

  default Scope scope() {
    return new Scope(6, 6, 6, 6);
  }

  default void assertSat(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(formula, synthesisEncoder.getHistoryAtoms());
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertSat(List<ExecutionFormula<E>> formulas) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(formulas, synthesisEncoder.getHistoryAtoms());
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertUnsat(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(formula, synthesisEncoder.getHistoryAtoms());
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertFact(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module =
        this.encoder().encode(formula.not(), synthesisEncoder.getHistoryAtoms());
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertSatWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(formula, synthesisEncoder.getHistoryAtoms());
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encodeWoTotalOrder().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertUnsatWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(formula, synthesisEncoder.getHistoryAtoms());
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encodeWoTotalOrder().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertFactWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module =
        this.encoder().encode(formula.not(), synthesisEncoder.getHistoryAtoms());
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encodeWoTotalOrder().solve(new Solver());
    assertTrue(sol.unsat());
  }
}
