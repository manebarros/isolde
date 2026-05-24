package haslab.isolde.core.check.candidate;

import haslab.isolde.core.general.DirectHistoryConstraintProblem;

public class CandCheckProblem extends DirectHistoryConstraintProblem<ContextualizedInstance> {

  public CandCheckProblem(ContextualizedInstance input) {
    super(input, DefaultCandCheckingEncoder.instance());
  }
}
