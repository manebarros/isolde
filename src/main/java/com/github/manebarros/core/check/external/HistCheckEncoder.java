package com.github.manebarros.core.check.external;

import com.github.manebarros.core.Execution;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.history.History;
import com.github.manebarros.kodkod.KodkodProblem;
import java.util.Collections;
import kodkod.engine.KodkodSolver;
import kodkod.engine.Solution;

public class HistCheckEncoder<E extends Execution> {
  private final HistCheckHistoryEncoder historyEncoder;
  private final HistCheckModuleEncoder<E> moduleEncoder;

  public HistCheckEncoder(
      HistCheckHistoryEncoder historyEncoder, HistCheckModuleEncoder<E> moduleEncoderConstructor) {
    this.historyEncoder = historyEncoder;
    this.moduleEncoder = moduleEncoderConstructor;
  }

  public HistCheckEncoder(
      HistCheckHistoryEncoder historyEncoder,
      HistCheckModuleEncoderConstructor<E> moduleEncoderConstructor) {
    this.historyEncoder = historyEncoder;
    this.moduleEncoder = moduleEncoderConstructor.generate(1);
  }

  public E execution() {
    return moduleEncoder.executions(historyEncoder.encoding()).get(0);
  }

  public KodkodProblem encode(History history, ExecutionFormula<E> formula) {
    HistCheckProblem problem = new HistCheckProblem(history, this.historyEncoder);
    problem.register(this.moduleEncoder, Collections.singletonList(formula));
    return problem.encode();
  }

  public Solution solve(History history, ExecutionFormula<E> formula, KodkodSolver solver) {
    return this.encode(history, formula).solve(solver);
  }
}
