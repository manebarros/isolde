package com.github.manebarros.generic.cerone;

import com.github.manebarros.AbstractHistoryK;
import com.github.manebarros.DirectAbstractHistoryEncoding;
import com.github.manebarros.generic.Execution;
import kodkod.ast.Relation;

public record CeroneExecution(Relation vis, Relation ar) implements Execution {

  @Override
  public AbstractHistoryK history() {
    return DirectAbstractHistoryEncoding.instance();
  }
}
