package haslab.isolde;

import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.noSession.DefaultSimpleHistorySynthesisEncoder;
import kodkod.instance.TupleSet;

public class DefaultSimpleHistorySynthesisEncoderTest implements SimpleHistorySynthesisEncoderTest {

  @Override
  public HistoryEncoder<FolSynthesisInput, TupleSet> encoder() {
    return new DefaultSimpleHistorySynthesisEncoder();
  }
}
