package haslab.isolde.core.synth;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.HistoryFormula;
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
