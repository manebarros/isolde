package com.github.manebarros;

import com.github.manebarros.core.synth.DefaultHistorySynthesisEncoder;
import com.github.manebarros.core.synth.HistorySynthesisEncoder;

public class DefaultHistorySynthesisEncoderTest implements HistorySynthesisEncoderTest {

  @Override
  public HistorySynthesisEncoder encoder() {
    return new DefaultHistorySynthesisEncoder();
  }
}
