package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryRel;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface HistoryEncoder<I extends Input, T> {
  AbstractHistoryRel encoding();

  Formula encode(I input, T extra, Bounds bounds);
}
