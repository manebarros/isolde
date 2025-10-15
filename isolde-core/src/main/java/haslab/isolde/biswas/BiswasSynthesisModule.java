package haslab.isolde.biswas;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.general.ExecutionModule;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.kodkod.KodkodUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class BiswasSynthesisModule
    implements ExecutionModule<
        BiswasExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>> {

  private final List<Relation> commitOrders;

  public BiswasSynthesisModule(int executions) {
    this.commitOrders = new ArrayList<>();
    for (int i = 0; i < executions; i++) {
      Relation commitOrder = Relation.binary("co#" + i);
      commitOrders.add(commitOrder);
    }
  }

  @Override
  public List<BiswasExecution> executions(AbstractHistoryK historyEncoding) {
    return this.commitOrders.stream()
        .map(co -> new BiswasExecution(historyEncoding, co))
        .collect(Collectors.toList());
  }

  @Override
  public int executions() {
    return commitOrders.size();
  }

  @Override
  public SimpleContext<HistoryAtoms> createContext(FolSynthesisInput input) {
    return new SimpleContext<>(input.historyAtoms());
  }

  @Override
  public Formula encode(
      Bounds b,
      List<ExecutionFormula<BiswasExecution>> formulas,
      SimpleContext<HistoryAtoms> context,
      Optional<TupleSet> totalOrderInfo,
      AbstractHistoryRel history) {
    return totalOrderInfo.isPresent()
        ? encode(b, formulas, context.val(), totalOrderInfo.get(), history)
        : encode(b, formulas, context.val(), history);
  }

  public Formula encode(
      Bounds b,
      List<ExecutionFormula<BiswasExecution>> formulas,
      HistoryAtoms historyAtoms,
      AbstractHistoryK history) {
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
              .and(commitOrderSemantics(history, commitOrder))
              .and(formulas.get(i).resolve(new BiswasExecution(history, commitOrder)));
    }
    return formula;
  }

  public Formula encode(
      Bounds b,
      List<ExecutionFormula<BiswasExecution>> formulas,
      HistoryAtoms historyAtoms,
      TupleSet txnTotalOrderTs,
      AbstractHistoryK history) {

    TupleFactory f = b.universe().factory();
    Relation mainCommitOrder = this.commitOrders.get(0);
    b.boundExactly(mainCommitOrder, txnTotalOrderTs);

    TupleSet sessionOrderLowerBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));

    Formula formula =
        Formula.and(
            history.sessionOrder().union(history.binaryWr()).in(mainCommitOrder),
            formulas.get(0).resolve(new BiswasExecution(history, mainCommitOrder)));

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
              .and(commitOrderSemantics(history, commitOrder))
              .and(formulas.get(i).resolve(new BiswasExecution(history, commitOrder)));
    }
    return formula;
  }

  private Formula commitOrderSemantics(AbstractHistoryK history, Expression commitOrder) {
    return Formula.and(
        KodkodUtil.transitive(commitOrder),
        KodkodUtil.total(commitOrder, history.transactions()),
        history.sessionOrder().union(history.binaryWr()).in(commitOrder));
  }
}
