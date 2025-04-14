package com.github.manebarros.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kodkod.ast.Formula;

public record SynthesisSpec<E extends Execution>(
    List<ExecutionFormula<E>> existentialFormulas, ExecutionFormula<E> universalFormula) {

  public static <E extends Execution> SynthesisSpec<E> fromUniversal(
      ExecutionFormula<E> universalConstraint) {
    return new SynthesisSpec<>(new ArrayList<>(), universalConstraint);
  }

  public SynthesisSpec(List<ExecutionFormula<E>> existentialConstraints) {
    this(existentialConstraints, e -> Formula.TRUE);
  }

  public SynthesisSpec(ExecutionFormula<E> existentialConstraint) {
    this(Collections.singletonList(existentialConstraint), e -> Formula.TRUE);
  }

  public SynthesisSpec(ExecutionFormula<E> existFormula, ExecutionFormula<E> univFormula) {
    this(Collections.singletonList(existFormula), univFormula);
  }
}
