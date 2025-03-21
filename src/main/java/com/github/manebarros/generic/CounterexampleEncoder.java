package com.github.manebarros.generic;

import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public interface CounterexampleEncoder<E extends Execution> {
  Formula guide(Instance instance, E execution, Bounds bounds);
}
