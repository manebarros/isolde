package haslab.isolde.core.check.external;

import haslab.isolde.core.general.DirectHistoryConstraintProblem;

public class HistCheckProblem
    extends DirectHistoryConstraintProblem<CheckingIntermediateRepresentation> {

  public HistCheckProblem(CheckingIntermediateRepresentation input) {
    super(input, DefaultHistoryCheckingEncoder.instance());
  }
}
