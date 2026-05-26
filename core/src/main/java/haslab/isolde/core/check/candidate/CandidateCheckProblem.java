package haslab.isolde.core.check.candidate;

import haslab.isolde.core.general.DirectHistoryConstraintProblem;
import haslab.isolde.core.general.HistoryEncoder;

public class CandidateCheckProblem extends DirectHistoryConstraintProblem<ContextualizedInstance> {

  public CandidateCheckProblem(ContextualizedInstance input) {
    this(input, DefaultCandCheckingEncoder.instance());
  }

  public CandidateCheckProblem(
      ContextualizedInstance input, HistoryEncoder<ContextualizedInstance> encoder) {
    super(input, encoder);
  }
}
