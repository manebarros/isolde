package com.github.manebarros.core.synth;

import com.github.manebarros.core.Execution;

import java.util.List;

public interface SynthesisModule<E extends Execution> extends SynthesisProblemExtender {
  List<E> executions();
}
