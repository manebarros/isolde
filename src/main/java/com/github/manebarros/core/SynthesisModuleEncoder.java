package com.github.manebarros.core;

import java.util.Collections;
import java.util.List;

public interface SynthesisModuleEncoder<E extends Execution> {
  SynthesisModule<E> encode(List<ExecutionFormula<E>> formulas, HistoryAtoms atoms);

  default SynthesisModule<E> encode(ExecutionFormula<E> formula, HistoryAtoms atoms) {
    return encode(Collections.singletonList(formula), atoms);
  }
}
