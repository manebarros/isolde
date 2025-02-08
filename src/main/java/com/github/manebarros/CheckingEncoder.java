package com.github.manebarros;

import kodkod.instance.Instance;

@FunctionalInterface
public interface CheckingEncoder {
  Contextualized<KodkodProblem> encode(
      AbstractHistoryK encoding, Instance instance, ExecutionFormulaG formula);
}
