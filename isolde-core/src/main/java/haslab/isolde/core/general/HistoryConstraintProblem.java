package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.kodkod.KodkodProblem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.instance.Bounds;
import kodkod.instance.Universe;

public class HistoryConstraintProblem<I extends AtomsContainer, T, S> {
  private final I input;
  private final List<ExecutionModuleInstance<?, I, S, ?>> extenders;

  private HistoryEncoder<T> histEncoder;
  private HelperStructureProducer<I, T> helperStructureProducer;
  private ProblemExtendingStrategy<T, S> problemExtendingStrategy;

  public HistoryConstraintProblem(
      I input,
      HistoryEncoder<T> historyEncoder,
      HelperStructureProducer<I, T> helperStructureProducer,
      ProblemExtendingStrategy<T, S> problemExtendingStrategy) {
    this.input = input;
    this.histEncoder = historyEncoder;
    this.extenders = new ArrayList<>();
    this.helperStructureProducer = helperStructureProducer;
    this.problemExtendingStrategy = problemExtendingStrategy;
  }

  public <E extends Execution, A extends AtomsContainer> List<E> register(
      ExecutionModuleConstructor<E, I, S, A> moduleConstructor,
      List<ExecutionFormula<E>> formulas) {
    ExecutionModule<E, I, S, A> module = moduleConstructor.build(formulas.size());
    A atoms = module.createContext(this.input);
    this.extenders.add(new ExecutionModuleInstance<>(module, atoms, formulas));
    return module.executions(histEncoder.encoding());
  }

  public <E extends Execution, A extends AtomsContainer> List<E> register(
      ExecutionModuleConstructor<E, I, S, A> moduleConstructor, ExecutionFormula<E> formula) {
    ExecutionModule<E, I, S, A> module = moduleConstructor.build(1);
    A atoms = module.createContext(this.input);
    this.extenders.add(
        new ExecutionModuleInstance<>(module, atoms, Collections.singletonList(formula)));
    return module.executions(histEncoder.encoding());
  }

  public <E extends Execution, A extends AtomsContainer> void register(
      ExecutionModule<E, I, S, A> module, List<ExecutionFormula<E>> formulas) {
    A atoms = module.createContext(this.input);
    this.extenders.add(new ExecutionModuleInstance<>(module, atoms, formulas));
  }

  public KodkodProblem encode() {
    List<Object> atoms = new ArrayList<>(input.atoms());
    for (var extender : extenders) {
      atoms.addAll(extender.context().atoms());
    }
    Universe u = new Universe(atoms);
    Bounds b = new Bounds(u);

    T extra = helperStructureProducer.produce(this.input, u);
    Formula formula = this.histEncoder.encode(extra, b);
    formula =
        problemExtendingStrategy.extend(formula, b, extra, histEncoder.encoding(), this.extenders);

    return new KodkodProblem(formula, b);
  }

  public Solution solve(Solver solver) {
    return encode().solve(solver);
  }

  public HistoryConstraintProblem<I, T, S> histEncoder(HistoryEncoder<T> historyEncoder) {
    this.histEncoder = historyEncoder;
    return this;
  }

  public HistoryConstraintProblem<I, T, S> helperStructureProducer(
      HelperStructureProducer<I, T> val) {
    this.helperStructureProducer = val;
    return this;
  }

  public HistoryConstraintProblem<I, T, S> problemExtendingStrategy(
      ProblemExtendingStrategy<T, S> val) {
    this.problemExtendingStrategy = val;
    return this;
  }

  public AbstractHistoryK historyEncoding() {
    return this.histEncoder.encoding();
  }
}
