package haslab.isolde.core.check.candidate;

import haslab.isolde.core.general.DirectHistoryConstraintProblem;
import haslab.isolde.core.general.HistoryEncoder;

public class CandCheckProblem extends DirectHistoryConstraintProblem<ContextualizedInstance> {

  public CandCheckProblem(ContextualizedInstance input) {
    this(input, DefaultCandCheckingEncoder.instance());
  }

  public CandCheckProblem(
      ContextualizedInstance input, HistoryEncoder<ContextualizedInstance> encoder) {
    super(input, encoder);
  }
}
