package haslab.isolde.core;

import kodkod.instance.Instance;

public interface Execution {
  HistorySchema history();

  String showAdditionalStructures(Instance instance);
}
