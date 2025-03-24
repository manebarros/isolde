package com.github.manebarros;

import com.github.manebarros.core.DefaultHistorySynthesisEncoder;
import com.github.manebarros.core.HistorySynthesisEncoder;

public class DefaultHistorySynthesisEncoderTest implements HistorySynthesisEncoderTest {

  @Override
  public HistorySynthesisEncoder encoder() {
    return new DefaultHistorySynthesisEncoder();
  }
}
