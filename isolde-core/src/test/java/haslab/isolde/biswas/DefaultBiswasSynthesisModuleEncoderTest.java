package haslab.isolde.biswas;

import haslab.isolde.core.synth.SynthesisModuleEncoder;

public class DefaultBiswasSynthesisModuleEncoderTest implements BiswasSynthesisModuleEncoderTest {

  @Override
  public SynthesisModuleEncoder<BiswasExecution> encoder() {
    return (historyEncoding, historyAtoms, formulas) ->
        new BiswasSynthesisModule(historyEncoding, historyAtoms, formulas);
  }
}
