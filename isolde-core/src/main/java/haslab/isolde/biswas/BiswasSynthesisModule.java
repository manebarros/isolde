package haslab.isolde.biswas;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.core.synth.SynthesisModule;
import haslab.isolde.kodkod.KodkodUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class BiswasSynthesisModule implements SynthesisModule<BiswasExecution> {
  private final AbstractHistoryK history;
  private List<Relation> commitOrders;
  private List<ExecutionFormula<BiswasExecution>> formulas;
  private HistoryAtoms historyAtoms;

  public BiswasSynthesisModule(
      AbstractHistoryK history,
      HistoryAtoms historyAtoms,
      List<ExecutionFormula<BiswasExecution>> formulas) {
    this.history = history;
    this.formulas = formulas;
    this.historyAtoms = historyAtoms;
    this.commitOrders = new ArrayList<>(formulas.size());
    for (int i = 0; i < formulas.size(); i++) {
      Relation commitOrder = Relation.binary("co#" + i);
      commitOrders.add(commitOrder);
    }
  }

  @Override
  public Formula extend(Bounds b) {
    TupleFactory f = b.universe().factory();

    TupleSet sessionOrderLowerBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));

    Formula formula = Formula.TRUE;

    for (int i = 0; i < commitOrders.size(); i++) {
      Relation commitOrder = this.commitOrders.get(i);
      TupleSet commitOrderTs = f.noneOf(2);
      commitOrderTs.addAll(sessionOrderLowerBound);
      for (int j = 0; j < historyAtoms.normalTxns().size(); j++) {
        for (int k = 0; k < historyAtoms.normalTxns().size(); k++) {
          if (j != k) {
            commitOrderTs.add(
                f.tuple(historyAtoms.normalTxns().get(j), historyAtoms.normalTxns().get(k)));
          }
        }
      }
      b.bound(commitOrder, sessionOrderLowerBound, commitOrderTs);
      formula =
          formula
              .and(commitOrderSemantics(commitOrder))
              .and(formulas.get(i).resolve(new BiswasExecution(history, commitOrder)));
    }
    return formula;
  }

  @Override
  public Formula extend(Bounds b, TupleSet txnTotalOrderTs) {
    TupleFactory f = b.universe().factory();
    Relation mainCommitOrder = this.commitOrders.get(0);
    b.boundExactly(mainCommitOrder, txnTotalOrderTs);

    TupleSet sessionOrderLowerBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));

    Formula formula =
        Formula.and(
            history.sessionOrder().union(history.binaryWr()).in(mainCommitOrder),
            this.formulas.get(0).resolve(new BiswasExecution(history, mainCommitOrder)));

    // TODO: Can we use a more strict upper bound for the remaining commit orders?
    for (int i = 1; i < formulas.size(); i++) {
      Relation commitOrder = this.commitOrders.get(i);
      TupleSet commitOrderTs = f.noneOf(2);
      commitOrderTs.addAll(sessionOrderLowerBound);
      for (int j = 0; j < historyAtoms.normalTxns().size(); j++) {
        for (int k = 0; k < historyAtoms.normalTxns().size(); k++) {
          if (j != k) {
            commitOrderTs.add(
                f.tuple(historyAtoms.normalTxns().get(j), historyAtoms.normalTxns().get(k)));
          }
        }
      }
      b.bound(commitOrder, sessionOrderLowerBound, commitOrderTs);
      formula =
          formula
              .and(commitOrderSemantics(commitOrder))
              .and(formulas.get(i).resolve(new BiswasExecution(history, commitOrder)));
    }
    return formula;
  }

  private Formula commitOrderSemantics(Expression commitOrder) {
    return Formula.and(
        KodkodUtil.transitive(commitOrder),
        KodkodUtil.total(commitOrder, history.transactions()),
        history.sessionOrder().union(history.binaryWr()).in(commitOrder));
  }

  @Override
  public Collection<Object> extraAtoms() {
    return new ArrayList<>();
  }

  @Override
  public List<BiswasExecution> executions() {
    return this.commitOrders.stream()
        .map(co -> new BiswasExecution(history, co))
        .collect(Collectors.toList());
  }
}
