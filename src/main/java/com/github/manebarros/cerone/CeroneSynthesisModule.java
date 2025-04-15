package com.github.manebarros.cerone;

import static com.github.manebarros.kodkod.KodkodUtil.total;
import static com.github.manebarros.kodkod.KodkodUtil.transitive;

import com.github.manebarros.core.AbstractHistoryK;
import com.github.manebarros.core.DirectAbstractHistoryEncoding;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.synth.HistoryAtoms;
import com.github.manebarros.core.synth.SynthesisModule;
import com.github.manebarros.kodkod.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class CeroneSynthesisModule implements SynthesisModule<CeroneExecution> {
  private AbstractHistoryK history;
  private List<VisAndArRels> executions;
  private List<ExecutionFormula<CeroneExecution>> formulas;
  private HistoryAtoms historyAtoms;

  private record VisAndArRels(Relation vis, Relation ar) {}

  public CeroneSynthesisModule(
      AbstractHistoryK history,
      HistoryAtoms historyAtoms,
      List<ExecutionFormula<CeroneExecution>> formulas) {
    this.history = history;
    this.formulas = formulas;
    this.historyAtoms = historyAtoms;
    this.executions = new ArrayList<>();
    for (int i = 0; i < formulas.size(); i++) {
      executions.add(new VisAndArRels(Relation.binary("vis#" + i), Relation.binary("ar#" + i)));
    }
  }

  @Override
  public List<CeroneExecution> executions() {
    List<CeroneExecution> r = new ArrayList<>();
    for (var p : this.executions) {
      r.add(new CeroneExecution(this.history, p.vis(), p.ar()));
    }
    return r;
  }

  @Override
  public Formula extend(Bounds b) {
    TupleFactory f = b.universe().factory();
    TupleSet commitOrderTs = Util.irreflexiveBound(f, historyAtoms.normalTxns());
    TupleSet visAndArLowerBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));
    commitOrderTs.addAll(visAndArLowerBound);
    Formula formula = Formula.TRUE;

    var enc = DirectAbstractHistoryEncoding.instance();

    for (int i = 0; i < formulas.size(); i++) {
      var execution = executions.get(i);
      Relation vis = execution.vis();
      Relation ar = execution.ar();
      b.bound(vis, visAndArLowerBound, commitOrderTs);
      b.bound(ar, visAndArLowerBound, commitOrderTs);
      formula =
          formula
              .and(vis.in(ar))
              .and(transitive(ar))
              .and(enc.sessionOrder().in(ar))
              .and(total(ar, enc.transactions()))
              .and(formulas.get(i).resolve(new CeroneExecution(this.history, vis, ar)));
    }
    return formula;
  }

  @Override
  public Formula extend(Bounds b, TupleSet txnTotalOrderTs) {
    TupleFactory tf = b.universe().factory();
    TupleSet visLb =
        tf.setOf(historyAtoms.initialTxn()).product(tf.setOf(historyAtoms.normalTxns().toArray()));

    b.boundExactly(executions.get(0).ar(), txnTotalOrderTs);
    b.bound(executions.get(0).vis(), visLb, txnTotalOrderTs);
    Formula formula =
        formulas
            .get(0)
            .resolve(
                new CeroneExecution(this.history, executions.get(0).vis(), executions.get(0).ar()));

    var enc = DirectAbstractHistoryEncoding.instance();

    TupleSet commitOrderTs = Util.irreflexiveBound(tf, historyAtoms.normalTxns());
    commitOrderTs.addAll(visLb);
    for (int i = 1; i < formulas.size(); i++) {
      var execution = executions.get(i);
      Relation vis = execution.vis();
      Relation ar = execution.ar();
      b.bound(vis, visLb, commitOrderTs);
      b.bound(ar, visLb, commitOrderTs);
      formula =
          formula
              .and(vis.in(ar))
              .and(transitive(ar))
              .and(enc.sessionOrder().in(ar))
              .and(total(ar, enc.transactions()))
              .and(formulas.get(i).resolve(new CeroneExecution(this.history, vis, ar)));
    }
    return formula;
  }

  @Override
  public Collection<Object> extraAtoms() {
    return new ArrayList<>();
  }
}
