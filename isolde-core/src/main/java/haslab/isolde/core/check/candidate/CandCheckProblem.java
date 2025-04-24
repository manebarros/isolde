package haslab.isolde.core.check.candidate;

import haslab.isolde.core.*;
import haslab.isolde.core.check.CheckingProblemExtender;
import haslab.isolde.kodkod.KodkodProblem;
import haslab.isolde.kodkod.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.Universe;

public class CandCheckProblem {
  private final Instance instance;
  private final AbstractHistoryK context;

  private final CandCheckHistoryEncoder checkingEncoder;
  private final List<CheckingProblemExtender> extenders;

  public CandCheckProblem(
      Instance instance, AbstractHistoryK context, CandCheckHistoryEncoder checkingEncoder) {
    this.instance = instance;
    this.context = context;
    this.checkingEncoder = checkingEncoder;
    this.extenders = new ArrayList<>();
  }

  public <E extends Execution> void register(
      CandCheckModuleEncoder<E> moduleEncoder, ExecutionFormula<E> formula) {
    this.register(moduleEncoder, Collections.singletonList(formula));
  }

  public <E extends Execution> void register(
      CandCheckModuleEncoder<E> moduleEncoder, List<ExecutionFormula<E>> formulas) {
    CheckingProblemExtender checkingModule =
        moduleEncoder.encode(
            this.instance, this.context, this.checkingEncoder.encoding(), formulas);
    register(checkingModule);
  }

  public void register(CheckingProblemExtender extender) {
    this.extenders.add(extender);
  }

  public KodkodProblem encode() {
    Evaluator eval = new Evaluator(instance);
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(Util.unaryTupleSetToAtoms(eval.evaluate(context.transactions())));
    atoms.addAll(Util.unaryTupleSetToAtoms(eval.evaluate(context.sessions())));
    atoms.addAll(Util.unaryTupleSetToAtoms(eval.evaluate(context.keys())));
    atoms.addAll(Util.unaryTupleSetToAtoms(eval.evaluate(context.values())));
    for (var extender : extenders) {
      atoms.addAll(extender.extraAtoms());
    }
    Bounds b = new Bounds(new Universe(atoms));

    Formula formula = this.checkingEncoder.encode(instance, context, b);
    for (var extender : this.extenders) {
      formula = formula.and(extender.extend(b));
    }

    return new KodkodProblem(formula, b);
  }
}
