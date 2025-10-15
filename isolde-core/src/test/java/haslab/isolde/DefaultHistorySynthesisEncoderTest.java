package haslab.isolde;

import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.synth.DefaultHistorySynthesisEncoder;
import haslab.isolde.core.synth.FolSynthesisProblem;

public class DefaultHistorySynthesisEncoderTest implements HistorySynthesisEncoderTest {

  @Override
  public HistoryEncoder<FolSynthesisProblem.InputWithTotalOrder> encoder() {
    return new DefaultHistorySynthesisEncoder();
  }
}
