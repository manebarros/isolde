package com.github.manebarros;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.manebarros.core.synth.DefaultHistorySynthesisEncoder;
import com.github.manebarros.core.Execution;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.synth.FolSynthesisEncoder;
import com.github.manebarros.core.synth.Scope;
import com.github.manebarros.core.synth.SynthesisModule;
import com.github.manebarros.core.synth.SynthesisModuleEncoder;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import org.junit.jupiter.api.Test;

public interface SynthesisModuleEncoderTest<E extends Execution> {
  SynthesisModuleEncoder<E> encoder();

  default Scope scope() {
    return new Scope(6, 6, 6, 6);
  }

  default void assertSat(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(synthesisEncoder, formula);
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertSat(List<ExecutionFormula<E>> formulas) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(synthesisEncoder, formulas);
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encode().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertUnsat(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(synthesisEncoder, formula);
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertFact(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(synthesisEncoder, formula.not());
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encode().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertSatWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(synthesisEncoder, formula);
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encodeWoTotalOrder().solve(new Solver());
    assertTrue(sol.sat());
  }

  default void assertUnsatWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(synthesisEncoder, formula);
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encodeWoTotalOrder().solve(new Solver());
    assertTrue(sol.unsat());
  }

  default void assertFactWoTotalOrder(ExecutionFormula<E> formula) {
    FolSynthesisEncoder synthesisEncoder =
        new FolSynthesisEncoder(new DefaultHistorySynthesisEncoder(), scope());
    SynthesisModule<E> module = this.encoder().encode(synthesisEncoder, formula.not());
    synthesisEncoder.register(module);
    Solution sol = synthesisEncoder.encodeWoTotalOrder().solve(new Solver());
    assertTrue(sol.unsat());
  }

  @Test
  default void itIsImpossibleToSynthesizeTriviallyUnsatFormula() {
    assertUnsat(e -> Formula.FALSE);
  }
}
