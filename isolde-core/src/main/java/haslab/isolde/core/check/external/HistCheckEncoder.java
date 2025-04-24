package haslab.isolde.core.check.external;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.history.History;
import haslab.isolde.kodkod.KodkodProblem;
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
