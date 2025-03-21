package com.github.manebarros.generic;

import kodkod.ast.Formula;

@FunctionalInterface
public interface ExecutionFormula<E extends Execution> {
  Formula resolve(E execution);

  default ExecutionFormula<E> and(ExecutionFormula<E> f) {
    return e -> this.resolve(e).and(f.resolve(e));
  }

  default ExecutionFormula<E> not() {
    return e -> this.resolve(e).not();
  }
}
