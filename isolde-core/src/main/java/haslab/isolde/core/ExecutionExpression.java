package haslab.isolde.core;

import kodkod.ast.Expression;
import kodkod.ast.Formula;

import java.util.List;

@FunctionalInterface
public interface ExecutionExpression<E extends Execution> {
  Expression resolve(E execution);
}
