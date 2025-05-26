package haslab.isolde.core.check.candidate;

import haslab.isolde.core.general.simple.HistoryConstraintProblemS;
import haslab.isolde.core.general.simple.HistoryEncoderS;

public class CandCheckProblem extends HistoryConstraintProblemS<ContextualizedInstance> {

  public CandCheckProblem(
      ContextualizedInstance input, HistoryEncoderS<ContextualizedInstance> historyEncoder) {
    super(input, historyEncoder);
  }
}
