package com.github.manebarros.generic;

import java.util.List;

public interface SynthesisEncoder<E extends Execution> {
  SynthesisModule<E> getModule(List<ExecutionFormula<E>> formulas, HistoryAtoms atoms);
}
