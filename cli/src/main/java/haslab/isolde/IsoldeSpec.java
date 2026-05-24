package haslab.isolde;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kodkod.ast.Formula;

public class IsoldeSpec {
  private final HistoryFormula historyFormula;
  private final Optional<SynthesisSpec<CeroneExecution>> ceroneSpec;
  private final Optional<SynthesisSpec<BiswasExecution>> biswasSpec;

  public static class Builder {
    private HistoryFormula historyFormula = h -> Formula.TRUE;

    // Cerone
    private boolean usesCerone = false;
    private final List<ExecutionFormula<CeroneExecution>> ceroneExistentials = new ArrayList<>();
    private ExecutionFormula<CeroneExecution> ceroneUniversal = null;

    // Biswas
    private boolean usesBiswas = false;
    private final List<ExecutionFormula<BiswasExecution>> biswasExistentials = new ArrayList<>();
    private ExecutionFormula<BiswasExecution> biswasUniversal = null;

    public Builder(IsoldeConstraint constraint) {
      and(constraint);
    }

    public Builder and(IsoldeConstraint constraint) {
      switch (constraint) {
        case IsoldeConstraint.HistoryConstraint(HistoryFormula formula):
          this.historyFormula = this.historyFormula.and(formula);
          return this;

        case IsoldeConstraint.CeroneConstraint(ExecutionFormula<CeroneExecution> formula):
          this.usesCerone = true;
          this.ceroneExistentials.add(formula);
          return this;

        case IsoldeConstraint.BiswasConstraint(ExecutionFormula<BiswasExecution> formula):
          this.usesBiswas = true;
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
          this.usesCerone = true;
          ceroneUniversal =
              ceroneUniversal != null ? ceroneUniversal.and(formula.not()) : formula.not();
          return this;

        case IsoldeConstraint.BiswasConstraint(ExecutionFormula<BiswasExecution> formula):
          this.usesBiswas = true;
          this.biswasUniversal =
              biswasUniversal != null ? biswasUniversal.and(formula.not()) : formula.not();
          return this;
      }
    }

    public IsoldeSpec build() {
      return new IsoldeSpec(this);
    }
  }

  private IsoldeSpec(Builder builder) {
    this.historyFormula = builder.historyFormula;

    this.ceroneSpec =
        builder.usesCerone
            ? Optional.of(new SynthesisSpec<>(builder.ceroneExistentials, builder.ceroneUniversal))
            : Optional.empty();

    this.biswasSpec =
        builder.usesBiswas
            ? Optional.of(new SynthesisSpec<>(builder.biswasExistentials, builder.biswasUniversal))
            : Optional.empty();
  }

  public HistoryFormula getHistoryFormula() {
    return historyFormula;
  }

  public Optional<SynthesisSpec<CeroneExecution>> getCeroneSpec() {
    return ceroneSpec;
  }

  public Optional<SynthesisSpec<BiswasExecution>> getBiswasSpec() {
    return biswasSpec;
  }

  public boolean usesCerone() {
    return ceroneSpec.isPresent();
  }

  public boolean usesBiswas() {
    return biswasSpec.isPresent();
  }
}
