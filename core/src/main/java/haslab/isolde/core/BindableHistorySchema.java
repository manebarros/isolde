package haslab.isolde.core;

import kodkod.ast.Relation;

public interface BindableHistorySchema extends HistorySchema {
  @Override
  Relation transactions();

  @Override
  Relation initialTransaction();
}
