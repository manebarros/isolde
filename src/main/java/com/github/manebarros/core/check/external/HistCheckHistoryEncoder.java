package com.github.manebarros.core.check.external;

import com.github.manebarros.core.AbstractHistoryK;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface HistCheckHistoryEncoder {
  AbstractHistoryK encoding();

  Formula encode(CheckingIntermediateRepresentation intermediateRepresentation, Bounds bounds);
}
