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
  private final HistoryEncoder<Candidate> historyEncoder;
  private final DirectExecutionModule<E, Candidate, ?> moduleEncoder;

  public DefaultCandidateChecker(DirectExecutionModule<E, Candidate, ?> moduleEncoder) {
    this.historyEncoder = CandidateHistoryEncoder.INSTANCE;
    this.moduleEncoder = moduleEncoder;
  }

  public DefaultCandidateChecker(
      HistoryEncoder<Candidate> historyEncoder,
      DirectExecutionModule<E, Candidate, ?> moduleEncoder) {
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
        new CandidateCheckProblem(new Candidate(context, instance), this.historyEncoder);
    problem.register(this.moduleEncoder, Arrays.asList(formula));
    return problem.encode();
  }
}
