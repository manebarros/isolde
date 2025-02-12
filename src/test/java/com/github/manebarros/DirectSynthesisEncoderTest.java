package com.github.manebarros;

public class DirectSynthesisEncoderTest implements SynthesisEncoderTest {

  @Override
  public SynthesisEncoder encoder() {
    return DirectSynthesisEncoder.instance();
  }
}
