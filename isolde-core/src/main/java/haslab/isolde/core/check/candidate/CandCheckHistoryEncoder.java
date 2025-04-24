package haslab.isolde.core.check.candidate;

import haslab.isolde.core.AbstractHistoryK;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public interface CandCheckHistoryEncoder {
  AbstractHistoryK encoding();

  Formula encode(Instance instance, AbstractHistoryK context, Bounds bounds);
}
