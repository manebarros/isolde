package com.github.manebarros.generic;

import com.github.manebarros.History;
import com.github.manebarros.KodkodProblem;
import kodkod.instance.Instance;

public interface CheckingEncoder<E extends Execution> {
  E execution();

  KodkodProblem encode(Instance instance, ExecutionFormula<E> formula);

  KodkodProblem encode(History history, ExecutionFormula<E> formula);
}
