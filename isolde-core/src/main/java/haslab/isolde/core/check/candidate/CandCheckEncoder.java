package haslab.isolde.core.check.candidate;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.kodkod.KodkodProblem;
import kodkod.engine.KodkodSolver;
import kodkod.engine.Solution;
import kodkod.instance.Instance;

public class CandCheckEncoder<E extends Execution> {
  private final CandCheckHistoryEncoder historyEncoder;
  private final CandCheckModuleEncoder<E> moduleEncoder;

  public CandCheckEncoder(
      CandCheckHistoryEncoder historyEncoder, CandCheckModuleEncoder<E> moduleEncoder) {
    this.historyEncoder = historyEncoder;
    this.moduleEncoder = moduleEncoder;
  }

  public CandCheckEncoder(
      CandCheckHistoryEncoder historyEncoder,
      CandCheckModuleEncoderConstructor<E> moduleEncoderConstructor) {
    this.historyEncoder = historyEncoder;
    this.moduleEncoder = moduleEncoderConstructor.generate(1);
  }

  public E execution() {
    return moduleEncoder.executions(historyEncoder.encoding()).get(0);
  }

  public KodkodProblem encode(
      Instance instance, AbstractHistoryK context, ExecutionFormula<E> formula) {
    CandCheckProblem problem = new CandCheckProblem(instance, context, this.historyEncoder);
    problem.register(this.moduleEncoder, formula);
    return problem.encode();
  }

  public Solution solve(
      Instance instance,
      AbstractHistoryK context,
      ExecutionFormula<E> formula,
      KodkodSolver solver) {
    return this.encode(instance, context, formula).solve(solver);
  }
}
