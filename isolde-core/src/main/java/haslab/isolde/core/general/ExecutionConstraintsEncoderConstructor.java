package haslab.isolde.core.general;

import haslab.isolde.core.Execution;

public interface ExecutionConstraintsEncoderConstructor<I extends Input, T, E extends Execution> {
  ExecutionConstraintsEncoder<I, T, E> generate(int executions);
}
