package haslab.isolde.core.check;

import java.util.Collection;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface CheckingProblemExtender {
  Collection<Object> extraAtoms();

  Formula extend(Bounds b);
}
