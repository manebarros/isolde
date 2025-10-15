package haslab.isolde.core.synth;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.HistoryDecls;
import haslab.isolde.core.general.ExecutionModuleInstance;
import haslab.isolde.core.general.HelperStructureProducer;
import haslab.isolde.core.general.HistoryConstraintProblem;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.general.ProblemExtendingStrategy;
import haslab.isolde.core.synth.FolSynthesisProblem.InputWithTotalOrder;
import java.util.List;
import java.util.Optional;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public class FolSynthesisProblem
    extends HistoryConstraintProblem<FolSynthesisInput, InputWithTotalOrder, Optional<TupleSet>> {

  public static record InputWithTotalOrder(FolSynthesisInput input, TupleSet totalOrder) {
    public InputWithTotalOrder(FolSynthesisInput input, Universe u) {
      this(input, extraForHistoryEncoding(input, u));
    }
  }

  public static TupleSet extraForHistoryEncoding(FolSynthesisInput input, Universe u) {
    TupleFactory f = u.factory();
    HistoryAtoms historyAtoms = input.historyAtoms();
    TupleSet txnTotalOrder = f.noneOf(2);
    // Traverse the transaction indexes from the initial transaction (i=0) to the penultimate one
    for (int i = 0; i < historyAtoms.getTxnAtoms().size() - 1; i++) {
      for (int j = i + 1; j < historyAtoms.getTxnAtoms().size(); j++) {
        txnTotalOrder.add(
            f.tuple(historyAtoms.getTxnAtoms().get(i), historyAtoms.getTxnAtoms().get(j)));
      }
    }
    return txnTotalOrder;
  }

  private static Formula applyExistentialQuantifier(
      Formula formula, HistoryDecls decls, AbstractHistoryK encoding) {
    return formula.forSome(decls.resolve(encoding));
  }

  public static Formula extend(
      Formula formula,
      Bounds b,
      InputWithTotalOrder extra,
      AbstractHistoryRel history,
      List<? extends ExecutionModuleInstance<?, ?, Optional<TupleSet>, ?>> extenders) {
    if (!extenders.isEmpty()) {
      formula = extenders.get(0).encode(b, Optional.of(extra.totalOrder()), history);
      for (int i = 1; i < extenders.size(); i++) {
        formula = formula.and(extenders.get(i).encode(b, Optional.empty(), history));
      }
    }

    HistoryDecls decls = extra.input().historyDecls();
    if (decls != null) {
      formula = applyExistentialQuantifier(formula, decls, history);
    }

    return formula;
  }

  public static Formula extendWithNoTotalOrder(
      Formula formula,
      Bounds b,
      InputWithTotalOrder extra,
      AbstractHistoryRel history,
      List<? extends ExecutionModuleInstance<?, ?, Optional<TupleSet>, ?>> extenders) {
    for (var extender : extenders) {
      formula = formula.and(extender.encode(b, Optional.empty(), history));
    }

    HistoryDecls decls = extra.input().historyDecls();
    if (decls != null) {
      formula = applyExistentialQuantifier(formula, decls, history);
    }
    return formula;
  }

  private FolSynthesisProblem(
      FolSynthesisInput input,
      HistoryEncoder<InputWithTotalOrder> historyEncoder,
      HelperStructureProducer<FolSynthesisInput, InputWithTotalOrder> helperStructureProducer,
      ProblemExtendingStrategy<InputWithTotalOrder, Optional<TupleSet>> problemExtendingStrategy) {
    super(input, historyEncoder, helperStructureProducer, problemExtendingStrategy);
  }

  public static FolSynthesisProblem withNoTotalOrder(FolSynthesisInput input) {
    return new FolSynthesisProblem(
        input,
        new DefaultHistorySynthesisEncoder(),
        InputWithTotalOrder::new,
        FolSynthesisProblem::extend);
  }

  public static FolSynthesisProblem withTotalOrder(FolSynthesisInput input) {
    return new FolSynthesisProblem(
        input,
        new DefaultHistorySynthesisEncoder(),
        InputWithTotalOrder::new,
        FolSynthesisProblem::extendWithNoTotalOrder);
  }

  public static FolSynthesisProblem withTotalOrder(Scope scope) {
    return withTotalOrder(new FolSynthesisInput.Builder(scope).build());
  }

  public static FolSynthesisProblem withNoTotalOrder(Scope scope) {
    return withNoTotalOrder(new FolSynthesisInput.Builder(scope).build());
  }
}
