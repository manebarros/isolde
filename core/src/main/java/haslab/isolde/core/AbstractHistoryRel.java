package haslab.isolde.core;

import kodkod.ast.Relation;

public interface AbstractHistoryRel extends HistorySchema {
  @Override
  Relation transactions();

  @Override
  Relation initialTransaction();
}
