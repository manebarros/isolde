package haslab.isolde.core.general;

import java.util.ArrayList;
import java.util.Collection;

public record SimpleContext<T>(T val) implements AtomsContainer {

  @Override
  public Collection<Object> atoms() {
    return new ArrayList<>();
  }
}
