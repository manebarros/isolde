package com.github.manebarros.core.check.external;

import com.github.manebarros.core.Execution;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.check.CheckingProblemExtender;
import com.github.manebarros.history.History;
import com.github.manebarros.kodkod.KodkodProblem;
import java.util.ArrayList;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.Universe;

public class HistCheckProblem {
  private final CheckingIntermediateRepresentation intermediateRepresentation;

  private final HistCheckHistoryEncoder checkingEncoder;
  private final List<CheckingProblemExtender> extenders;

  public HistCheckProblem(History hist, HistCheckHistoryEncoder checkingEncoder) {
    this.intermediateRepresentation = new CheckingIntermediateRepresentation(hist);
    this.checkingEncoder = checkingEncoder;
    this.extenders = new ArrayList<>();
  }

  public <E extends Execution> void register(
      HistCheckModuleEncoder<E> moduleEncoder, List<ExecutionFormula<E>> formulas) {
    register(moduleEncoder.encode(this, formulas));
  }

  public void register(CheckingProblemExtender extender) {
    this.extenders.add(extender);
  }

  public KodkodProblem encode() {
    List<Object> atoms = intermediateRepresentation.atoms();
    for (var extender : extenders) {
      atoms.addAll(extender.extraAtoms());
    }
    Bounds b = new Bounds(new Universe(atoms));

    Formula formula = this.checkingEncoder.encode(intermediateRepresentation, b);
    for (var extender : this.extenders) {
      formula = formula.and(extender.extend(b));
    }

    return new KodkodProblem(formula, b);
  }

  public CheckingIntermediateRepresentation getIntermediateRepresentation() {
    return this.intermediateRepresentation;
  }

  public HistCheckHistoryEncoder getHistoryCheckingEncoder() {
    return this.checkingEncoder;
  }
}
