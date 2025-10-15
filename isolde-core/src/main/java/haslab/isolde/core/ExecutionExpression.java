package haslab.isolde.core;

import kodkod.ast.Expression;

@FunctionalInterface
public interface ExecutionExpression<E extends Execution> {
  Expression resolve(E execution);
}
