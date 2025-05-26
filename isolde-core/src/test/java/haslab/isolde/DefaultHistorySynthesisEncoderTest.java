package haslab.isolde;

import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.synth.DefaultHistorySynthesisEncoder;
import haslab.isolde.core.synth.FolSynthesisInput;
import kodkod.instance.TupleSet;

public class DefaultHistorySynthesisEncoderTest implements HistorySynthesisEncoderTest {

  @Override
  public HistoryEncoder<FolSynthesisInput, TupleSet> encoder() {
    return new DefaultHistorySynthesisEncoder();
  }
}
