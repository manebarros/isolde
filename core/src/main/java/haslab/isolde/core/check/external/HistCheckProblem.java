package haslab.isolde.core.check.external;

import haslab.isolde.core.general.DirectHistoryConstraintProblem;
import haslab.isolde.core.general.HistoryEncoder;

public class HistCheckProblem
    extends DirectHistoryConstraintProblem<CheckingIntermediateRepresentation> {

  public HistCheckProblem(CheckingIntermediateRepresentation input) {
    this(input, DefaultHistoryCheckingEncoder.INSTANCE);
  }

  public HistCheckProblem(
      CheckingIntermediateRepresentation input,
      HistoryEncoder<CheckingIntermediateRepresentation> encoder) {
    super(input, encoder);
  }
}
