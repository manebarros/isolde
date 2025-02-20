package com.github.manebarros;

import kodkod.instance.Instance;

public interface CheckingEncoder<E extends DatabaseExecution> {
  Contextualized<KodkodProblem> encode(
      AbstractHistoryK encoding, Instance instance, ExecutionFormulaK<E> formula);

  Contextualized<KodkodProblem> encode(History history, ExecutionFormulaK<E> formula);
}
