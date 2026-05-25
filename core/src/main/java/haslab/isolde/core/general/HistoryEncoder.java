package haslab.isolde.core.general;

import haslab.isolde.core.BindableHistorySchema;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface HistoryEncoder<SC> {
  BindableHistorySchema encoding();

  Formula encode(SC sharedContext, Bounds bounds);
}
