package com.github.manebarros.core;

import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.TupleSet;

public interface HistorySynthesisEncoder {
  AbstractHistoryK encoding();

  Formula encode(
      HistoryAtoms atoms, HistoryFormula formula, TupleSet txnTotalOrderTs, Bounds bounds);

  default Formula encode(HistoryAtoms atoms, TupleSet txnTotalOrderTs, Bounds bounds) {
    return encode(atoms, h -> Formula.TRUE, txnTotalOrderTs, bounds);
  }
}
