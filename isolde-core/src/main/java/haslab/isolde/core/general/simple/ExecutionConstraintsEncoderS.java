package haslab.isolde.core.general.simple;


import haslab.isolde.core.Execution;
import haslab.isolde.core.general.ExecutionConstraintsEncoder;
import haslab.isolde.core.general.Input;

public interface ExecutionConstraintsEncoderS<I extends Input, E extends Execution>
    extends ExecutionConstraintsEncoder<I, Void, E> {}
