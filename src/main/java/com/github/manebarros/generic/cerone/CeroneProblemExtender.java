package com.github.manebarros.generic.cerone;

import static com.github.manebarros.KodkodUtil.total;
import static com.github.manebarros.KodkodUtil.transitive;

import com.github.manebarros.DirectAbstractHistoryEncoding;
import com.github.manebarros.Util;
import com.github.manebarros.generic.ExecutionFormula;
import com.github.manebarros.generic.HistoryAtoms;
import com.github.manebarros.generic.ProblemExtender;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class CeroneProblemExtender implements ProblemExtender {
  private List<CeroneExecution> executions;
  private List<ExecutionFormula<CeroneExecution>> formulas;
  private HistoryAtoms historyAtoms;

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
              .and(formulas.get(i).resolve(new CeroneExecution(vis, ar)));
    }
    return formula;
  }

  // TODO
  @Override
  public Formula extend(Bounds b, TupleSet txnTotalOrderTs) {
    return null;
  }
}
