package com.github.manebarros;

import kodkod.ast.Expression;
import kodkod.ast.Formula;

@FunctionalInterface
public interface ExecutionFormulaG {
  Formula apply(AbstractHistoryK history, Expression commitOrder);

  default ExecutionFormulaG not() {
    return (h, co) -> this.apply(h, co).not();
  }
}
