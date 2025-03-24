package com.github.manebarros.core;

import java.util.Collections;
import java.util.List;

public interface SynthesisModuleEncoder<E extends Execution> {
  SynthesisModule<E> encode(
      AbstractHistoryK historyEncoding, HistoryAtoms atoms, List<ExecutionFormula<E>> formulas);

  default SynthesisModule<E> encode(
      FolSynthesisEncoder synthesisEncoder, ExecutionFormula<E> formula) {
    return encode(
        synthesisEncoder.getHistoryEncoding(),
        synthesisEncoder.getHistoryAtoms(),
        Collections.singletonList(formula));
  }

  default SynthesisModule<E> encode(
      FolSynthesisEncoder synthesisEncoder, List<ExecutionFormula<E>> formulas) {
    return encode(
        synthesisEncoder.getHistoryEncoding(), synthesisEncoder.getHistoryAtoms(), formulas);
  }

  default SynthesisModule<E> encode(
      AbstractHistoryK historyEncoding, HistoryAtoms atoms, ExecutionFormula<E> formula) {
    return encode(historyEncoding, atoms, Collections.singletonList(formula));
  }
}
