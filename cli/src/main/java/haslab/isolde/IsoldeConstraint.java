package haslab.isolde;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;

public sealed interface IsoldeConstraint {

  record HistoryConstraint(HistoryFormula formula) implements IsoldeConstraint {}

  record CeroneConstraint(ExecutionFormula<CeroneExecution> formula) implements IsoldeConstraint {}

  record BiswasConstraint(ExecutionFormula<BiswasExecution> formula) implements IsoldeConstraint {}

  static HistoryConstraint history(HistoryFormula f) {
    return new HistoryConstraint(f);
  }

  static CeroneConstraint cerone(ExecutionFormula<CeroneExecution> f) {
    return new CeroneConstraint(f);
  }

  static BiswasConstraint biswas(ExecutionFormula<BiswasExecution> f) {
    return new BiswasConstraint(f);
  }

  default IsoldeSpec.Builder and(IsoldeConstraint f) {
    return new IsoldeSpec.Builder(this).and(f);
  }

  default IsoldeSpec.Builder andNot(IsoldeConstraint f) {
    return new IsoldeSpec.Builder(this).andNot(f);
  }

  default IsoldeSpec asSpec() {
    return new IsoldeSpec.Builder(this).build();
  }
}
