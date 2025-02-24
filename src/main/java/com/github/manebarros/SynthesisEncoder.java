package com.github.manebarros;

import java.util.Collections;
import java.util.List;

public interface SynthesisEncoder {
  Contextualized<KodkodProblem> encode(
      Scope scope,
      HistoryFormula historyFormula,
      List<ExecutionFormulaK<BiswasExecutionK>> biswasFormulas,
      List<ExecutionFormulaK<CeroneExecutionK>> ceroneFormulas);

  Contextualized<KodkodProblem> encode(
      Scope scope,
      List<ExecutionFormulaK<BiswasExecutionK>> biswasFormulas,
      List<ExecutionFormulaK<CeroneExecutionK>> ceroneFormulas);

  default Contextualized<KodkodProblem> encode(Scope scope) {
    return encode(
        scope,
        Collections.singletonList(ExecutionFormulaK::trivial),
        Collections.singletonList(ExecutionFormulaK::trivial));
  }
}
