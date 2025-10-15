package haslab.isolde.core.synth;

import haslab.isolde.core.HistoryDecls;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.general.AtomsContainer;
import java.util.List;
import kodkod.ast.Formula;

public record FolSynthesisInput(
    HistoryAtoms historyAtoms, HistoryFormula historyFormula, HistoryDecls historyDecls)
    implements AtomsContainer {

  public static class Builder {
    private final HistoryAtoms historyAtoms;

    private HistoryFormula formula = h -> Formula.TRUE;
    private HistoryDecls decls = null;

    public Builder(Scope scope) {
      this.historyAtoms = new HistoryAtoms(scope);
    }

    public Builder formula(HistoryFormula formula) {
      this.formula = formula;
      return this;
    }

    public Builder delcs(HistoryDecls decls) {
      this.decls = decls;
      return this;
    }

    public FolSynthesisInput build() {
      return new FolSynthesisInput(this.historyAtoms, this.formula, this.decls);
    }
  }

  @Override
  public List<Object> atoms() {
    return historyAtoms().atoms();
  }
}
