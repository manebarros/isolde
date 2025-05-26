package haslab.isolde.core.general.simple;

import haslab.isolde.core.general.ProblemExtender;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface ProblemExtenderS extends ProblemExtender<Void> {
  Formula extend(Bounds b);

  @Override
  default Formula extend(Void none, Bounds b) {
    return extend(b);
  }
}
