package com.github.manebarros;

import kodkod.instance.Instance;

public interface CheckingEncoder {
  Contextualized<KodkodProblem> encode(
      AbstractHistoryK encoding, Instance instance, ExecutionFormulaG formula);

  Contextualized<KodkodProblem> encode(History history, ExecutionFormulaG formula);
}
