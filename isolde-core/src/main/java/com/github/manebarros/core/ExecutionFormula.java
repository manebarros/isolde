package com.github.manebarros.core;

import java.util.List;
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

  static <E extends Execution> ExecutionFormula<E> and(List<ExecutionFormula<E>> f) {
    if (f.isEmpty()) {
      return e -> Formula.TRUE;
    }
    ExecutionFormula<E> r = f.get(0);
    for (int i = 1; i < f.size(); i++) {
      r = r.and(f.get(i));
    }
    return r;
  }
}
