package com.github.manebarros;

import kodkod.instance.Instance;

public interface CheckingEncoder<E> {
  CheckingContextualized<E, KodkodProblem> encode(
      AbstractHistoryK encoding, Instance instance, ExecutionFormulaK<E> formula);

  CheckingContextualized<E, KodkodProblem> encode(History history, ExecutionFormulaK<E> formula);
}
