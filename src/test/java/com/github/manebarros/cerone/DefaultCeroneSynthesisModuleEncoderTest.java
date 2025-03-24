package com.github.manebarros.cerone;

import com.github.manebarros.core.SynthesisModuleEncoder;

public class DefaultCeroneSynthesisModuleEncoderTest implements CeroneSynthesisModuleEncoderTest {

  @Override
  public SynthesisModuleEncoder<CeroneExecution> encoder() {
    return (historyEncoding, historyAtoms, formulas) ->
        new CeroneSynthesisModule(historyEncoding, formulas, historyAtoms);
  }
}
