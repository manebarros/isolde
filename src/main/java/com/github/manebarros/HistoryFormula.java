package com.github.manebarros;

import kodkod.ast.Formula;

@FunctionalInterface
public interface HistoryFormula {
  Formula apply(AbstractHistoryK history);
}
