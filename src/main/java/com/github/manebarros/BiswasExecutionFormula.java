package com.github.manebarros;

import kodkod.ast.Expression;
import kodkod.ast.Formula;

@FunctionalInterface
public interface BiswasExecutionFormula {
  Formula apply(AbstractHistoryK history, Expression commitOrder);

  default BiswasExecutionFormula not() {
    return (h, co) -> this.apply(h, co).not();
  }

  public static BiswasExecutionFormula trivial() {
    return (h, co) -> Formula.TRUE;
  }
}
