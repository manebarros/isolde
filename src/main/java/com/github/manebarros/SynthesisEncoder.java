package com.github.manebarros;

import static kodkod.ast.Formula.TRUE;

import java.util.Collections;
import java.util.List;

@FunctionalInterface
public interface SynthesisEncoder {
  Contextualized<KodkodProblem> encode(Scope scope, List<ExecutionFormulaG> formulas);

  default Contextualized<KodkodProblem> encode(Scope scope) {
    return encode(scope, Collections.singletonList((h, c) -> TRUE));
  }

  default Contextualized<KodkodProblem> encode(Scope scope, ExecutionFormulaG formula) {
    return encode(scope, Collections.singletonList(formula));
  }
}
