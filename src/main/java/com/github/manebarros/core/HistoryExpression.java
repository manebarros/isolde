package com.github.manebarros.core;

import kodkod.ast.Expression;

@FunctionalInterface
public interface HistoryExpression {
  Expression resolve(AbstractHistoryK history);
}
