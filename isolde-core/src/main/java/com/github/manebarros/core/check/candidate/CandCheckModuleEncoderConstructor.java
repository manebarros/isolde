package com.github.manebarros.core.check.candidate;

import com.github.manebarros.core.Execution;

@FunctionalInterface
public interface CandCheckModuleEncoderConstructor<E extends Execution> {
  CandCheckModuleEncoder<E> generate(int executions);
}
