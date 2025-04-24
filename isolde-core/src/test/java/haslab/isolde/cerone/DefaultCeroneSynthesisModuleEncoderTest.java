package haslab.isolde.cerone;

import haslab.isolde.core.synth.SynthesisModuleEncoder;

public class DefaultCeroneSynthesisModuleEncoderTest implements CeroneSynthesisModuleEncoderTest {

  @Override
  public SynthesisModuleEncoder<CeroneExecution> encoder() {
    return CeroneSynthesisModule::new;
  }
}
