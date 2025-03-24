package com.github.manebarros.core;

import kodkod.ast.Formula;

@FunctionalInterface
public interface HistoryFormula {
  Formula resolve(AbstractHistoryK history);

  default HistoryFormula not() {
    return h -> this.resolve(h).not();
  }

  default HistoryFormula and(HistoryFormula f) {
    return h -> this.resolve(h).and(f.resolve(h));
  }
}
