package haslab.isolde.core.check.candidate;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.general.Input;
import haslab.isolde.kodkod.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kodkod.engine.Evaluator;
import kodkod.instance.Instance;

public record ContextualizedInstance(AbstractHistoryK context, Instance instance) implements Input {

  @Override
  public Collection<Object> atoms() {
    Evaluator eval = new Evaluator(instance);
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(Util.unaryTupleSetToAtoms(eval.evaluate(context.transactions())));
    atoms.addAll(Util.unaryTupleSetToAtoms(eval.evaluate(context.sessions())));
    atoms.addAll(Util.unaryTupleSetToAtoms(eval.evaluate(context.keys())));
    atoms.addAll(Util.unaryTupleSetToAtoms(eval.evaluate(context.values())));
    return atoms;
  }
}
