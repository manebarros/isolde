package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryRel;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface HistoryEncoder<SC> {
  AbstractHistoryRel encoding();

  Formula encode(SC sharedContext, Bounds bounds);
}
