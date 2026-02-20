package haslab.isolde.core.cegis;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SynthesisSpec<E extends Execution> {
  private final List<ExecutionFormula<E>> existentialFormulas;
  private final Optional<ExecutionFormula<E>> universalFormula;

  public SynthesisSpec(
      List<ExecutionFormula<E>> existentialConstraints, ExecutionFormula<E> universalFormula) {
    this.existentialFormulas = existentialConstraints;
    this.universalFormula = Optional.ofNullable(universalFormula);
  }

  public SynthesisSpec(List<ExecutionFormula<E>> existentialConstraints) {
    this(existentialConstraints, null);
  }

  public SynthesisSpec(ExecutionFormula<E> existentialConstraint) {
    this(Collections.singletonList(existentialConstraint));
  }

  public SynthesisSpec(ExecutionFormula<E> existFormula, ExecutionFormula<E> univFormula) {
    this(Collections.singletonList(existFormula), univFormula);
  }

  public static <E extends Execution> SynthesisSpec<E> fromUniversal(
      ExecutionFormula<E> universalConstraint) {
    return new SynthesisSpec<>(new ArrayList<>(), universalConstraint);
  }

  public static <E extends Execution> SynthesisSpec<E> not(ExecutionFormula<E> formula) {
    return fromUniversal(formula.not());
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
