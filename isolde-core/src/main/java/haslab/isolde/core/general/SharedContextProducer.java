package haslab.isolde.core.general;

import kodkod.instance.Universe;

@FunctionalInterface
public interface SharedContextProducer<I extends AtomsContainer, T> {
  T produce(I input, Universe u);
}
