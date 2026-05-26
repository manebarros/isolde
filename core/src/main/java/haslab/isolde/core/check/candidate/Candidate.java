package haslab.isolde.core.check.candidate;

import haslab.isolde.core.HistorySchema;
import haslab.isolde.core.general.AtomsContainer;
import haslab.isolde.kodkod.Translations;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kodkod.engine.Evaluator;
import kodkod.instance.Instance;

public record Candidate(HistorySchema context, Instance instance) implements AtomsContainer {

  @Override
  public Collection<Object> atoms() {
    Evaluator eval = new Evaluator(instance);
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(Translations.unaryTupleSetToAtoms(eval.evaluate(context.transactions())));
    atoms.addAll(Translations.unaryTupleSetToAtoms(eval.evaluate(context.keys())));
    atoms.addAll(Translations.unaryTupleSetToAtoms(eval.evaluate(context.values())));
    return atoms;
  }
}
