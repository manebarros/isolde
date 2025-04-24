package haslab.isolde.core;

import kodkod.ast.Expression;

@FunctionalInterface
public interface HistoryExpression {
  Expression resolve(AbstractHistoryK history);
}
