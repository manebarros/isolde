package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public class ExecutionModuleInstance<
    E extends Execution, I extends AtomsContainer, S, C extends AtomsContainer> {
  private final ExecutionModule<E, I, S, C> module;
  private final C context;
  private final List<ExecutionFormula<E>> formulas;

  public ExecutionModuleInstance(
      ExecutionModule<E, I, S, C> module, C context, List<ExecutionFormula<E>> formulas) {
    this.module = module;
    this.context = context;
    this.formulas = formulas;
  }

  public Formula encode(Bounds bounds, S spec, AbstractHistoryRel history) {
    return module.encode(bounds, this.formulas, this.context, spec, history);
  }

  public C context() {
    return this.context;
  }
}
