package haslab.isolde.core.check.candidate;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistorySchema;
import haslab.isolde.core.general.DirectExecutionModule;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.kodkod.KodkodProblem;
import java.util.Arrays;
import kodkod.instance.Instance;

public class DefaultCandidateChecker<E extends Execution> implements CandidateChecker<E> {
  private final HistoryEncoder<ContextualizedInstance> historyEncoder;
  private final DirectExecutionModule<E, ContextualizedInstance, ?> moduleEncoder;

  public DefaultCandidateChecker(
      DirectExecutionModule<E, ContextualizedInstance, ?> moduleEncoder) {
    this.historyEncoder = DefaultCandCheckingEncoder.instance();
    this.moduleEncoder = moduleEncoder;
  }

  public DefaultCandidateChecker(
      HistoryEncoder<ContextualizedInstance> historyEncoder,
      DirectExecutionModule<E, ContextualizedInstance, ?> moduleEncoder) {
    this.historyEncoder = historyEncoder;
    this.moduleEncoder = moduleEncoder;
  }

  @Override
  public E execution() {
    return moduleEncoder.executions(historyEncoder.encoding()).get(0);
  }

  @Override
  public KodkodProblem encode(
      Instance instance, HistorySchema context, ExecutionFormula<E> formula) {
    CandidateCheckProblem problem =
        new CandidateCheckProblem(
            new ContextualizedInstance(context, instance), this.historyEncoder);
    problem.register(this.moduleEncoder, Arrays.asList(formula));
    return problem.encode();
  }
}
