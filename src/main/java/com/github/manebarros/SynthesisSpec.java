package com.github.manebarros;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kodkod.ast.Formula;

public class SynthesisSpec<E> {
  private final List<ExecutionFormulaK<E>> existentialConstraints;
  private final ExecutionFormulaK<E> universalConstraint;

  public static <E> SynthesisSpec<E> fromUniversal(ExecutionFormulaK<E> universalConstraint) {
    return new SynthesisSpec<>(new ArrayList<>(), universalConstraint);
  }

  public SynthesisSpec(ExecutionFormulaK<E> existentialConstraint) {
    this.existentialConstraints = Collections.singletonList(existentialConstraint);
    this.universalConstraint = (h, e) -> Formula.TRUE;
  }

  public SynthesisSpec(
      ExecutionFormulaK<E> existentialConstraint, ExecutionFormulaK<E> universalConstraint) {
    this.existentialConstraints = Collections.singletonList(existentialConstraint);
    this.universalConstraint = universalConstraint;
  }

  public SynthesisSpec(
      List<ExecutionFormulaK<E>> existentialConstraints, ExecutionFormulaK<E> universalConstraint) {
    this.existentialConstraints = existentialConstraints;
    this.universalConstraint = universalConstraint;
  }

  public List<ExecutionFormulaK<E>> getExistentialConstraints() {
    return existentialConstraints;
  }

  public ExecutionFormulaK<E> getUniversalConstraint() {
    return universalConstraint;
  }
}
