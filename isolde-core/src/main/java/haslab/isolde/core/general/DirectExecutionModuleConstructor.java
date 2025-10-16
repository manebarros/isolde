package haslab.isolde.core.general;

import haslab.isolde.core.Execution;

@FunctionalInterface
public interface DirectExecutionModuleConstructor<
        E extends Execution, I extends AtomsContainer, C extends AtomsContainer>
    extends ExecutionModuleConstructor<E, I, Void, C> {

  @Override
  DirectExecutionModule<E, I, C> build(int executions);
}
