package haslab.isolde.core.check.external;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.general.DirectExecutionModule;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.history.History;
import haslab.isolde.kodkod.KodkodProblem;
import java.util.Collections;

public class HistCheckEncoder<E extends Execution> implements HistCheckerI<E> {
  private final HistoryEncoder<CheckingIntermediateRepresentation> historyEncoder;
  private final DirectExecutionModule<E, CheckingIntermediateRepresentation, ?> moduleEncoder;

  public HistCheckEncoder(
      DirectExecutionModule<E, CheckingIntermediateRepresentation, ?> moduleEncoder) {
    this.historyEncoder = DefaultHistoryCheckingEncoder.instance();
    this.moduleEncoder = moduleEncoder;
  }

  @Override
  public E execution() {
    return moduleEncoder.executions(historyEncoder.encoding()).get(0);
  }

  @Override
  public KodkodProblem encode(History history, ExecutionFormula<E> formula) {
    HistCheckProblem problem =
        (HistCheckProblem)
            new HistCheckProblem(new CheckingIntermediateRepresentation(history))
                .histEncoder(historyEncoder);
    problem.register(this.moduleEncoder, Collections.singletonList(formula));
    return problem.encode();
  }
}
