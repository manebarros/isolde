package haslab.isolde.core.check.external;

import haslab.isolde.core.Execution;

@FunctionalInterface
public interface HistCheckModuleEncoderConstructor<E extends Execution> {
  HistCheckModuleEncoder<E> generate(int executions);
}
