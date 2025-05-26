package haslab.isolde.core.general;

import java.util.Collection;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface ProblemExtender<T> {
  Collection<Object> extraAtoms();

  Formula extend(T extra, Bounds b);
}
