package haslab.isolde.core.general;

import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

/**
 * Produces a helper object from an {@link AtomsContainer} and a {@link Universe}.
 *
 * <p>The produced object will usually contain one or more {@link TupleSet}s representing constant
 * relational values to be shared between the {@link HistoryEncoder} and the {@link
 * ExecutionModuleInstance}s of a {@link HistoryConstraintProblem}.
 *
 * <p>The produced object is meant to preserve all the information in the input relevant for both
 * the history encoder and the ExecutionModuleInstances.
 *
 * @param <I> the type of the input for a {@link HistoryConstraintProblem}
 * @param <T> the type of shared context object to be produced
 * @see HistoryConstraintProblem
 * @see AtomsContainer
 */
@FunctionalInterface
public interface SharedContextProducer<I extends AtomsContainer, SC> {
  SC produce(I input, Universe u);
}
