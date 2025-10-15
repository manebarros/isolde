package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryRel;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface HistoryEncoder<T> {
  AbstractHistoryRel encoding();

  Formula encode(T extra, Bounds bounds);
}
