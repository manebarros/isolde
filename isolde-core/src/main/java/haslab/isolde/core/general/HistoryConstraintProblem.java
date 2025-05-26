package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.kodkod.KodkodProblem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.Universe;

public class HistoryConstraintProblem<I extends Input, T, S> {
  private final I input;

  private final HistoryEncoder<I, T> histEncoder;
  private final List<ProblemExtender<S>> extenders;

  private final HelperStructureProducer<I, T> helperStructureProducer;
  private final ProblemExtendingStrategy<T, S> problemExtendingStrategy;

  public HistoryConstraintProblem(
      I input,
      HistoryEncoder<I, T> historyEncoder,
      HelperStructureProducer<I, T> helperStructureProducer,
      ProblemExtendingStrategy<T, S> problemExtendingStrategy) {
    this.input = input;
    this.histEncoder = historyEncoder;
    this.extenders = new ArrayList<>();
    this.helperStructureProducer = helperStructureProducer;
    this.problemExtendingStrategy = problemExtendingStrategy;
  }

  public void register(ProblemExtender<S> extender) {
    this.extenders.add(extender);
  }

  public <E extends Execution> void register(
      ExecutionConstraintsEncoder<I, S, E> moduleEncoder, List<ExecutionFormula<E>> formulas) {
    ProblemExtender<S> extender = moduleEncoder.encode(this, formulas);
    register(extender);
  }

  public <E extends Execution> void register(
      ExecutionConstraintsEncoder<I, S, E> moduleEncoder, ExecutionFormula<E> formula) {
    this.register(moduleEncoder, Collections.singletonList(formula));
  }

  public <E extends Execution> List<E> register(
      ExecutionConstraintsEncoderConstructor<I, S, E> moduleEncoderConstructor,
      List<ExecutionFormula<E>> formulas) {
    ExecutionConstraintsEncoder<I, S, E> moduleEncoder =
        moduleEncoderConstructor.generate(formulas.size());
    this.register(moduleEncoder, formulas);
    return moduleEncoder.executions(this.historyEncoding());
  }

  public <E extends Execution> E register(
      ExecutionConstraintsEncoderConstructor<I, S, E> moduleEncoderConstructor,
      ExecutionFormula<E> formula) {
    ExecutionConstraintsEncoder<I, S, E> moduleEncoder = moduleEncoderConstructor.generate(1);
    this.register(moduleEncoder, formula);
    return moduleEncoder.executions(this.historyEncoding()).get(0);
  }

  public <E extends Execution> List<E> register(
      ExecutionModule<I, S, E> module, List<ExecutionFormula<E>> formulas) {
    ContextualizedExtender<E, S> extender =
        module.encode(this.input, this.historyEncoding(), formulas);
    this.register(extender);
    return extender.executions();
  }

  public <E extends Execution> E register(
      ExecutionModule<I, S, E> module, ExecutionFormula<E> formula) {
    ContextualizedExtender<E, S> extender =
        module.encode(this.input, this.historyEncoding(), Collections.singletonList(formula));
    this.register(extender);
    return extender.executions().get(0);
  }

  public KodkodProblem encode() {
    List<Object> atoms = new ArrayList<>(input.atoms());
    for (var extender : extenders) {
      atoms.addAll(extender.extraAtoms());
    }
    Universe u = new Universe(atoms);
    Bounds b = new Bounds(u);

    T extra = helperStructureProducer.produce(this.input, u);
    Formula formula =
        this.histEncoder
            .encode(input, extra, b)
            .and(problemExtendingStrategy.extend(b, extra, this.extenders));

    return new KodkodProblem(formula, b);
  }

  public I getInput() {
    return input;
  }

  public HistoryEncoder<I, T> getHistEncoder() {
    return histEncoder;
  }

  public List<ProblemExtender<S>> getExtenders() {
    return extenders;
  }

  public AbstractHistoryK historyEncoding() {
    return this.histEncoder.encoding();
  }
}
