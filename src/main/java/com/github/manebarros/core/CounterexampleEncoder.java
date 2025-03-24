package com.github.manebarros.core;

import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public interface CounterexampleEncoder<E extends Execution> {
  HistoryFormula guide(Instance instance, E execution, ExecutionFormula<E> formula, Bounds bounds);
}
