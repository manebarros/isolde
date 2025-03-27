package com.github.manebarros.core;

import com.github.manebarros.kodkod.KodkodProblem;
import java.util.ArrayList;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public class FolSynthesisEncoder {
  private final HistoryAtoms historyAtoms;
  private final HistoryFormula historyFormula;

  private final HistorySynthesisEncoder histEncoder;
  private final List<SynthesisProblemExtender> extenders;

  public FolSynthesisEncoder(
      HistorySynthesisEncoder histEncoder, Scope scope, HistoryFormula historyFormula) {
    this.histEncoder = histEncoder;
    this.extenders = new ArrayList<>();
    this.historyAtoms = new HistoryAtoms(scope);
    this.historyFormula = historyFormula;
  }

  public FolSynthesisEncoder(Scope scope) {
    this(scope, h -> Formula.TRUE);
  }

  public FolSynthesisEncoder(HistorySynthesisEncoder histEncoder, Scope scope) {
    this(histEncoder, scope, h -> Formula.TRUE);
  }

  public FolSynthesisEncoder(Scope scope, HistoryFormula historyFormula) {
    this(new DefaultHistorySynthesisEncoder(), scope, historyFormula);
  }

  public HistoryAtoms getHistoryAtoms() {
    return historyAtoms;
  }

  public HistoryFormula getHistoryFormula() {
    return historyFormula;
  }

  public List<SynthesisProblemExtender> getExtenders() {
    return extenders;
  }

  public AbstractHistoryK getHistoryEncoding() {
    return this.histEncoder.encoding();
  }

  public void register(SynthesisProblemExtender extender) {
    this.extenders.add(extender);
  }

  public KodkodProblem encode() {
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(historyAtoms.atoms());
    for (var extender : extenders) {
      atoms.addAll(extender.extraAtoms());
    }
    Universe u = new Universe(atoms);
    Bounds b = new Bounds(u);
    TupleFactory f = u.factory();

    TupleSet txnTotalOrderTs = f.noneOf(2);
    // Traverse the txn indexes from the initial txn (i=0) to the penultimate txn
    for (int i = 0; i < historyAtoms.getTxnAtoms().size() - 1; i++) {
      for (int j = i + 1; j < historyAtoms.getTxnAtoms().size(); j++) {
        txnTotalOrderTs.add(
            f.tuple(historyAtoms.getTxnAtoms().get(i), historyAtoms.getTxnAtoms().get(j)));
      }
    }

    Formula formula =
        this.histEncoder.encode(this.historyAtoms, this.historyFormula, txnTotalOrderTs, b);
    if (!this.extenders.isEmpty()) {
      formula = formula.and(this.extenders.get(0).extend(b, txnTotalOrderTs));

      for (int i = 1; i < this.extenders.size(); i++) {
        formula = formula.and(this.extenders.get(i).extend(b));
      }
    }

    return new KodkodProblem(formula, b);
  }

  public KodkodProblem encodeWoTotalOrder() {
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(historyAtoms.atoms());
    for (var extender : extenders) {
      atoms.addAll(extender.extraAtoms());
    }
    Universe u = new Universe(atoms);
    Bounds b = new Bounds(u);
    TupleFactory f = u.factory();

    TupleSet txnTotalOrderTs = f.noneOf(2);
    // Traverse the txn indexes from the initial txn (i=0) to the penultimate txn
    for (int i = 0; i < historyAtoms.getTxnAtoms().size() - 1; i++) {
      for (int j = i + 1; j < historyAtoms.getTxnAtoms().size(); j++) {
        txnTotalOrderTs.add(
            f.tuple(historyAtoms.getTxnAtoms().get(i), historyAtoms.getTxnAtoms().get(j)));
      }
    }

    Formula formula =
        this.histEncoder.encode(this.historyAtoms, this.historyFormula, txnTotalOrderTs, b);
    for (var extender : this.extenders) {
      formula = formula.and(extender.extend(b));
    }

    return new KodkodProblem(formula, b);
  }
}
