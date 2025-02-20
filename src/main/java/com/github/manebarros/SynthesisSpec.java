package com.github.manebarros;

import java.util.List;

public class SynthesisSpec<E extends DatabaseExecution> {
  private final List<ExecutionFormulaK<E>> existentialConstraints;
  private final ExecutionFormulaK<E> universalConstraint;

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
