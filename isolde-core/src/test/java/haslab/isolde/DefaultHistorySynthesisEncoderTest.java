package haslab.isolde;

import haslab.isolde.core.synth.DefaultHistorySynthesisEncoder;
import haslab.isolde.core.synth.HistorySynthesisEncoder;

public class DefaultHistorySynthesisEncoderTest implements HistorySynthesisEncoderTest {

  @Override
  public HistorySynthesisEncoder encoder() {
    return new DefaultHistorySynthesisEncoder();
  }
}
