package haslab.isolde.core.synth;

import haslab.isolde.core.HistoryDecls;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.general.HistoryConstraintProblem;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.general.ProblemExtender;
import haslab.isolde.core.general.ProblemExtendingStrategy;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public class FolSynthesisProblem
    extends HistoryConstraintProblem<FolSynthesisInput, TupleSet, TransactionTotalOrderInfo> {

  public static TupleSet extraForHistoryEncoding(FolSynthesisInput input, Universe u) {
    TupleFactory f = u.factory();
    HistoryAtoms historyAtoms = input.historyAtoms();
    TupleSet txnTotalOrder = f.noneOf(2);
    // Traverse the txn indexes from the initial txn (i=0) to the penultimate txn
    for (int i = 0; i < historyAtoms.getTxnAtoms().size() - 1; i++) {
      for (int j = i + 1; j < historyAtoms.getTxnAtoms().size(); j++) {
        txnTotalOrder.add(
            f.tuple(historyAtoms.getTxnAtoms().get(i), historyAtoms.getTxnAtoms().get(j)));
      }
    }
    return txnTotalOrder;
  }

  public static Formula apply(
      Bounds b, TupleSet extra, List<ProblemExtender<TransactionTotalOrderInfo>> extenders) {
    Formula formula = Formula.TRUE;
    if (!extenders.isEmpty()) {
      formula = extenders.get(0).extend(new TransactionTotalOrderInfo(true, extra), b);

      for (int i = 1; i < extenders.size(); i++) {
        formula =
            formula.and(extenders.get(i).extend(new TransactionTotalOrderInfo(false, extra), b));
      }
    }
    return formula;
  }

  public static Formula applyWithNoTotalOrder(
      Bounds b, TupleSet extra, List<ProblemExtender<TransactionTotalOrderInfo>> extenders) {
    Formula formula = Formula.TRUE;
    for (var extender : extenders) {
      formula = formula.and(extender.extend(new TransactionTotalOrderInfo(false, extra), b));
    }
    return formula;
  }

  public static FolSynthesisProblem withNoTotalOrder(
      Scope scope, HistoryFormula hf, HistoryEncoder<FolSynthesisInput, TupleSet> histEncoder) {
    return new FolSynthesisProblem(
        scope, hf, histEncoder, FolSynthesisProblem::applyWithNoTotalOrder);
  }

  public static FolSynthesisProblem withNoTotalOrder(
      Scope scope, HistoryEncoder<FolSynthesisInput, TupleSet> histEncoder) {
    return new FolSynthesisProblem(
        scope, h -> Formula.TRUE, histEncoder, FolSynthesisProblem::applyWithNoTotalOrder);
  }

  public FolSynthesisProblem(
      Scope scope,
      HistoryFormula historyFormula,
      HistoryEncoder<FolSynthesisInput, TupleSet> histEncoder) {
    super(
        new FolSynthesisInput(new HistoryAtoms(scope), historyFormula),
        histEncoder,
        FolSynthesisProblem::extraForHistoryEncoding,
        FolSynthesisProblem::apply);
  }

  public FolSynthesisProblem(
      Scope scope,
      HistoryFormula historyFormula,
      HistoryDecls decls,
      HistoryEncoder<FolSynthesisInput, TupleSet> histEncoder) {
    super(
        new FolSynthesisInput(new HistoryAtoms(scope), historyFormula, decls),
        histEncoder,
        FolSynthesisProblem::extraForHistoryEncoding,
        FolSynthesisProblem::apply);
  }

  private FolSynthesisProblem(
      Scope scope,
      HistoryFormula historyFormula,
      HistoryEncoder<FolSynthesisInput, TupleSet> histEncoder,
      ProblemExtendingStrategy<TupleSet, TransactionTotalOrderInfo> problemExtendingStrategy) {
    super(
        new FolSynthesisInput(new HistoryAtoms(scope), historyFormula),
        histEncoder,
        FolSynthesisProblem::extraForHistoryEncoding,
        FolSynthesisProblem::apply);
  }

  public FolSynthesisProblem(Scope scope, HistoryFormula historyFormula) {
    this(scope, historyFormula, new DefaultHistorySynthesisEncoder());
  }

  public FolSynthesisProblem(Scope scope, HistoryFormula historyFormula, HistoryDecls decls) {
    this(scope, historyFormula, decls, new DefaultHistorySynthesisEncoder());
  }

  public FolSynthesisProblem(Scope scope, HistoryEncoder<FolSynthesisInput, TupleSet> histEncoder) {
    this(scope, h -> Formula.TRUE, histEncoder);
  }

  public FolSynthesisProblem(Scope scope) {
    this(scope, h -> Formula.TRUE);
  }
}
