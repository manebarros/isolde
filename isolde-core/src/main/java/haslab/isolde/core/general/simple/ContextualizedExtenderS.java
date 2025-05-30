package haslab.isolde.core.general.simple;

import haslab.isolde.core.Execution;
import java.util.List;

public interface ContextualizedExtenderS<E extends Execution> extends ProblemExtenderS {
  List<E> executions();
}
