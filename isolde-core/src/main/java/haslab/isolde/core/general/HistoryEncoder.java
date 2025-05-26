package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryK;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface HistoryEncoder<I extends Input, T> {
  AbstractHistoryK encoding();

  Formula encode(I input, T extra, Bounds bounds);
}
