package haslab.isolde.core.general.simple;

import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.general.Input;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public interface HistoryEncoderS<I extends Input> extends HistoryEncoder<I, Void> {
  Formula encode(I input, Bounds bounds);

  @Override
  default Formula encode(I input, Void none, Bounds bounds) {
    return encode(input, bounds);
  }
}
