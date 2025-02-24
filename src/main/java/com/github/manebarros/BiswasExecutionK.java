package com.github.manebarros;

import kodkod.ast.Expression;

@FunctionalInterface
public interface BiswasExecutionK {
  Expression commitOrder();

  static BiswasExecutionK build(Expression commitOrder) {
    return () -> commitOrder;
  }
}
