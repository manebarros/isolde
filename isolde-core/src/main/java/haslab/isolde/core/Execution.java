package haslab.isolde.core;

import kodkod.instance.Instance;

public interface Execution {
  AbstractHistoryK history();

  String showAdditionalStructures(Instance instance);
}
