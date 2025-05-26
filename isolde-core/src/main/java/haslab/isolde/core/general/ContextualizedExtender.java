package haslab.isolde.core.general;

import haslab.isolde.core.Execution;
import java.util.List;

public interface ContextualizedExtender<E extends Execution, T> extends ProblemExtender<T> {
  List<E> executions();
}
