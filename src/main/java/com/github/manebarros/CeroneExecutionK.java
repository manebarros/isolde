package com.github.manebarros;

import kodkod.ast.Expression;

public interface CeroneExecutionK {
  Expression vis();

  Expression ar();

  static CeroneExecutionK build(Expression vis, Expression ar) {
    return new CeroneExecutionK() {
      public Expression vis() {
        return vis;
      }

      public Expression ar() {
        return ar;
      }
    };
  }
}
