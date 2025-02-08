package com.github.manebarros;

import java.util.List;

@FunctionalInterface
public interface SynthesisEncoder {
  Contextualized<KodkodProblem> encode(Scope scope, List<ExecutionFormulaG> formulas);
}
