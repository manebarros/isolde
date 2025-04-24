package haslab.isolde.core.check.external;

import haslab.isolde.core.AbstractHistoryK;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface HistCheckHistoryEncoder {
  AbstractHistoryK encoding();

  Formula encode(CheckingIntermediateRepresentation intermediateRepresentation, Bounds bounds);
}
