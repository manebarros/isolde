package haslab.isolde.core.check.candidate;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.general.DirectExecutionModule;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.kodkod.KodkodProblem;
import java.util.Arrays;
import kodkod.instance.Instance;

public class CandChecker<E extends Execution> implements CandCheckerI<E> {
  private final HistoryEncoder<ContextualizedInstance> historyEncoder;
  private final DirectExecutionModule<E, ContextualizedInstance, ?> moduleEncoder;

  public CandChecker(DirectExecutionModule<E, ContextualizedInstance, ?> moduleEncoder) {
    this.historyEncoder = DefaultCandCheckingEncoder.instance();
    this.moduleEncoder = moduleEncoder;
  }

  public CandChecker(
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
      Instance instance, AbstractHistoryK context, ExecutionFormula<E> formula) {
    CandCheckProblem problem =
        (CandCheckProblem)
            new CandCheckProblem(new ContextualizedInstance(context, instance))
                .histEncoder(this.historyEncoder);
    problem.register(this.moduleEncoder, Arrays.asList(formula));
    return problem.encode();
  }
}
