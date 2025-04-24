package haslab.isolde.core.check.candidate;

import haslab.isolde.core.Execution;

@FunctionalInterface
public interface CandCheckModuleEncoderConstructor<E extends Execution> {
  CandCheckModuleEncoder<E> generate(int executions);
}
