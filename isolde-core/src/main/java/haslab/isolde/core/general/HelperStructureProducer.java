package haslab.isolde.core.general;

import kodkod.instance.Universe;

public interface HelperStructureProducer<I extends Input, T> {
  T produce(I input, Universe u);
}
