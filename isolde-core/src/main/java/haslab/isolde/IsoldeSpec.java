package haslab.isolde;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import java.util.ArrayList;
import java.util.List;
import kodkod.ast.Formula;

public class IsoldeSpec {
  private final HistoryFormula historyFormula;
  private final SynthesisSpec<CeroneExecution> ceroneSpec;
  private final SynthesisSpec<BiswasExecution> biswasSpec;

  public static class Builder {
    private HistoryFormula historyFormula = h -> Formula.TRUE;

    // Cerone
    private final List<ExecutionFormula<CeroneExecution>> ceroneExistentials = new ArrayList<>();
    private ExecutionFormula<CeroneExecution> ceroneUniversal = e -> Formula.TRUE;

    // Biswas
    private final List<ExecutionFormula<BiswasExecution>> biswasExistentials = new ArrayList<>();
    private ExecutionFormula<BiswasExecution> biswasUniversal = e -> Formula.TRUE;

    public Builder(IsoldeConstraint constraint) {
      and(constraint);
    }

    public Builder and(IsoldeConstraint constraint) {
      switch (constraint) {
        case IsoldeConstraint.HistoryConstraint(HistoryFormula formula):
          this.historyFormula = this.historyFormula.and(formula);
          return this;
        case IsoldeConstraint.CeroneConstraint(ExecutionFormula<CeroneExecution> formula):
          this.ceroneExistentials.add(formula);
          return this;
        case IsoldeConstraint.BiswasConstraint(ExecutionFormula<BiswasExecution> formula):
          this.biswasExistentials.add(formula);
          return this;
      }
    }

    public Builder andNot(IsoldeConstraint constraint) {
      switch (constraint) {
        case IsoldeConstraint.HistoryConstraint(HistoryFormula formula):
          this.historyFormula = this.historyFormula.and(formula.not());
          return this;

        case IsoldeConstraint.CeroneConstraint(ExecutionFormula<CeroneExecution> formula):
          this.ceroneUniversal = this.ceroneUniversal.and(formula.not());
          return this;

        case IsoldeConstraint.BiswasConstraint(ExecutionFormula<BiswasExecution> formula):
          this.biswasUniversal = this.biswasUniversal.and(formula.not());
          return this;
      }
    }

    public IsoldeSpec build() {
      return new IsoldeSpec(this);
    }
  }

  private IsoldeSpec(Builder builder) {
    this.historyFormula = builder.historyFormula;
    this.ceroneSpec = new SynthesisSpec<>(builder.ceroneExistentials, builder.ceroneUniversal);
    this.biswasSpec = new SynthesisSpec<>(builder.biswasExistentials, builder.biswasUniversal);
  }

  public HistoryFormula getHistoryFormula() {
    return historyFormula;
  }

  public SynthesisSpec<CeroneExecution> getCeroneSpec() {
    return ceroneSpec;
  }

  public SynthesisSpec<BiswasExecution> getBiswasSpec() {
    return biswasSpec;
  }
}
