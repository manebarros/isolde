package haslab.isolde.core.check.candidate;

import static haslab.isolde.kodkod.Formulas.asTupleSet;
import static haslab.isolde.kodkod.Translations.unaryTupleSetToAtoms;

import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.HistorySchema;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.kodkod.Translations;
import kodkod.ast.Formula;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;

/**
 * Encodes a {@link Candidate}'s history by binding the relations of {@link
 * DirectAbstractHistoryEncoding} to the tuple sets read from the candidate's Instance.
 */
public final class CandidateHistoryEncoder implements HistoryEncoder<Candidate> {

  public static final CandidateHistoryEncoder INSTANCE = new CandidateHistoryEncoder();

  private CandidateHistoryEncoder() {}

  @Override
  public DirectAbstractHistoryEncoding encoding() {
    return DirectAbstractHistoryEncoding.INSTANCE;
  }

  @Override
  public Formula encode(Candidate candidate, Bounds b) {
    Instance instance = candidate.instance();
    HistorySchema context = candidate.context();
    Evaluator ev = new Evaluator(instance);
    TupleFactory f = b.universe().factory();
    DirectAbstractHistoryEncoding enc = encoding();

    b.boundExactly(
        enc.transactions(),
        asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.transactions()))));
    b.boundExactly(enc.keys(), asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.keys()))));
    b.boundExactly(
        enc.values(), asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.values()))));
    b.boundExactly(
        enc.initialTransaction(),
        asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.initialTransaction()))));

    b.boundExactly(
        enc.finalWrites(), Translations.convert(ev, context, HistorySchema::finalWrites, f, 3));
    b.boundExactly(
        enc.externalReads(), Translations.convert(ev, context, HistorySchema::externalReads, f, 3));
    b.boundExactly(
        enc.sessionOrder(), Translations.convert(ev, context, HistorySchema::sessionOrder, f, 2));

    return Formula.TRUE;
  }
}
