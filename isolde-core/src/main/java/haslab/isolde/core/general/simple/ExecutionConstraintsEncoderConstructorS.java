package haslab.isolde.core.general.simple;


import haslab.isolde.core.Execution;
import haslab.isolde.core.general.ExecutionConstraintsEncoderConstructor;
import haslab.isolde.core.general.Input;

public interface ExecutionConstraintsEncoderConstructorS<I extends Input, E extends Execution>
    extends ExecutionConstraintsEncoderConstructor<I, Void, E> {

  @Override
  ExecutionConstraintsEncoderS<I, E> generate(int executions);
}
