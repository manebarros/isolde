package com.github.manebarros.core;

import com.github.manebarros.history.History;
import com.github.manebarros.kodkod.KodkodProblem;
import kodkod.instance.Instance;

public interface CheckingEncoder<E extends Execution> {
  E execution();

  KodkodProblem encode(AbstractHistoryK encoding, Instance instance, ExecutionFormula<E> formula);

  KodkodProblem encode(History history, ExecutionFormula<E> formula);
}
