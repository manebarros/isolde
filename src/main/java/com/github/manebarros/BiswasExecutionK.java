package com.github.manebarros;

import kodkod.ast.Expression;

public interface BiswasExecutionK extends DatabaseExecution {
  Expression commitOrder();
}
