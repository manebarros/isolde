package com.github.manebarros;

import kodkod.ast.Formula;

@FunctionalInterface
public interface ExecutionFormulaK<E> {
  Formula apply(AbstractHistoryK h, E execution);

  default ExecutionFormulaK<E> not() {
    return (h, e) -> this.apply(h, e).not();
  }

  default ExecutionFormulaK<E> and(ExecutionFormulaK<E> f) {
    return (h, e) -> this.apply(h, e).and(f.apply(h, e));
  }

  public static <E> Formula trivial(AbstractHistoryK h, E execution) {
    return Formula.TRUE;
  }
}
