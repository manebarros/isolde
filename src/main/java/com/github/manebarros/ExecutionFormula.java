package com.github.manebarros;

import kodkod.ast.Expression;
import kodkod.ast.Formula;

@FunctionalInterface
public interface ExecutionFormula {
  Formula apply(Expression commitOrder);
}
