package haslab.isolde.core.general;

import kodkod.instance.Universe;

@FunctionalInterface
public interface HelperStructureProducer<I extends AtomsContainer, T> {
  T produce(I input, Universe u);
}
