package haslab.isolde.core.synth;

import haslab.isolde.core.HistoryDecls;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.general.Input;
import java.util.List;
import java.util.Optional;
import kodkod.ast.Formula;

public record FolSynthesisInput(
    HistoryAtoms historyAtoms, HistoryFormula historyFormula, HistoryDecls historyDecls)
    implements Input {

  public FolSynthesisInput(HistoryAtoms historyAtoms, HistoryFormula historyFormula) {
    this(historyAtoms, historyFormula, null);
  }

  public FolSynthesisInput(HistoryAtoms historyAtoms) {
    this(historyAtoms, h -> Formula.TRUE);
  }

  @Override
  public List<Object> atoms() {
    return historyAtoms().atoms();
  }

  @Override
  public Optional<HistoryDecls> decls() {
    if (this.historyDecls == null) return Optional.empty();
    return Optional.of(historyDecls());
  }
}
