package com.github.manebarros.core.cegis;

import com.github.manebarros.core.Execution;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.HistoryFormula;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public interface CounterexampleEncoder<E extends Execution> {
  HistoryFormula guide(Instance instance, E execution, ExecutionFormula<E> formula, Bounds bounds);
}
