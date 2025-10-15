package haslab.isolde.core.general;

import haslab.isolde.core.Execution;

public interface ExecutionModuleConstructor<
    E extends Execution, I extends AtomsContainer, S, C extends AtomsContainer> {
  ExecutionModule<E, I, S, C> build(int executions);
}
