package com.github.manebarros.core.check.external;

import com.github.manebarros.core.Execution;

@FunctionalInterface
public interface HistCheckModuleEncoderConstructor<E extends Execution> {
  HistCheckModuleEncoder<E> generate(int executions);
}
