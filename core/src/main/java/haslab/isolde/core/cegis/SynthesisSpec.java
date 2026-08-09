package haslab.isolde.core.cegis;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import java.util.List;
import java.util.Optional;

public final class SynthesisSpec<E extends Execution> {
  private final List<ExecutionFormula<E>> existentialFormulas;
  private final Optional<ExecutionFormula<E>> universalFormula;

  public SynthesisSpec(List<ExecutionFormula<E>> existentials, ExecutionFormula<E> universal) {
    this.existentialFormulas = List.copyOf(existentials);
    this.universalFormula = Optional.ofNullable(universal);
  }

  @SafeVarargs
  public static <E extends Execution> SynthesisSpec<E> allowedBy(ExecutionFormula<E>... formulas) {
    return new SynthesisSpec<>(List.of(formulas), null);
  }

  public static <E extends Execution> SynthesisSpec<E> disallowedBy(ExecutionFormula<E> level) {
    return new SynthesisSpec<>(List.of(), level.not());
  }

  public SynthesisSpec<E> andDisallowedBy(ExecutionFormula<E> level) {
    if (this.universalFormula.isPresent()) {
      throw new IllegalStateException("SynthesisSpec already has a universal formula");
    }
    return new SynthesisSpec<>(this.existentialFormulas, level.not());
  }

  public List<ExecutionFormula<E>> existentialFormulas() {
    return existentialFormulas;
  }

  public Optional<ExecutionFormula<E>> universalFormula() {
    return universalFormula;
  }

  public boolean hasUniversal() {
    return this.universalFormula.isPresent();
  }
}
