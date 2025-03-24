package com.github.manebarros.biswas;

import com.github.manebarros.core.SynthesisModuleEncoder;

public class DefaultBiswasSynthesisModuleEncoderTest implements BiswasSynthesisModuleEncoderTest {

  @Override
  public SynthesisModuleEncoder<BiswasExecution> encoder() {
    return (historyEncoding, historyAtoms, formulas) ->
        new BiswasSynthesisModule(historyEncoding, historyAtoms, formulas);
  }
}
