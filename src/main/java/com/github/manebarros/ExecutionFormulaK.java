package com.github.manebarros;

import kodkod.ast.Formula;

@FunctionalInterface
public interface ExecutionFormulaK<E extends DatabaseExecution> {
  Formula apply(E execution);

  default ExecutionFormulaK<E> not() {
    return e -> this.apply(e).not();
  }

  default ExecutionFormulaK<E> and(ExecutionFormulaK<E> f) {
    return e -> this.apply(e).and(f.apply(e));
  }

  public static <E extends DatabaseExecution> Formula trivial(E execution) {
    return Formula.TRUE;
  }
}
