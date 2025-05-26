package haslab.isolde.core.check.candidate;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.general.simple.ExecutionConstraintsEncoderConstructorS;
import haslab.isolde.core.general.simple.ExecutionConstraintsEncoderS;
import haslab.isolde.core.general.simple.HistoryConstraintProblemS;
import haslab.isolde.core.general.simple.HistoryEncoderS;
import haslab.isolde.kodkod.KodkodProblem;
import kodkod.engine.KodkodSolver;
import kodkod.engine.Solution;
import kodkod.instance.Instance;

public class CandCheckEncoder<E extends Execution> {
  private final HistoryEncoderS<ContextualizedInstance> historyEncoder;
  private final ExecutionConstraintsEncoderS<ContextualizedInstance, E> moduleEncoder;

  public CandCheckEncoder(
      HistoryEncoderS<ContextualizedInstance> historyEncoder,
      ExecutionConstraintsEncoderS<ContextualizedInstance, E> moduleEncoder) {
    this.historyEncoder = historyEncoder;
    this.moduleEncoder = moduleEncoder;
  }

  public CandCheckEncoder(
      HistoryEncoderS<ContextualizedInstance> historyEncoder,
      ExecutionConstraintsEncoderConstructorS<ContextualizedInstance, E> moduleEncoderConstructor) {
    this.historyEncoder = historyEncoder;
    this.moduleEncoder = moduleEncoderConstructor.generate(1);
  }

  public E execution() {
    return moduleEncoder.executions(historyEncoder.encoding()).get(0);
  }

  public KodkodProblem encode(
      Instance instance, AbstractHistoryK context, ExecutionFormula<E> formula) {
    HistoryConstraintProblemS<ContextualizedInstance> problem =
        new HistoryConstraintProblemS<>(
            new ContextualizedInstance(context, instance), this.historyEncoder);
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
