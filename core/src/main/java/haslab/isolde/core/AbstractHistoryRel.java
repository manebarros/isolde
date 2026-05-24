package haslab.isolde.core;

import kodkod.ast.Relation;

public interface AbstractHistoryRel extends AbstractHistoryK {
  @Override
  Relation transactions();

  @Override
  Relation initialTransaction();
}
