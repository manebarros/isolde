package haslab.isolde.core.general;

import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface ProblemExtendingStrategy<T, S> {

  Formula extend(Bounds b, T helper, List<ProblemExtender<S>> extenders);
}
