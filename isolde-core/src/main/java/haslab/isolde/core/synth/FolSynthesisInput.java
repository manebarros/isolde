package haslab.isolde.core.synth;

import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.general.Input;
import java.util.List;
import kodkod.ast.Formula;

public record FolSynthesisInput(HistoryAtoms historyAtoms, HistoryFormula historyFormula)
    implements Input {

  public FolSynthesisInput(HistoryAtoms historyAtoms) {
    this(historyAtoms, h -> Formula.TRUE);
  }

  @Override
  public List<Object> atoms() {
    return historyAtoms().atoms();
  }
}
