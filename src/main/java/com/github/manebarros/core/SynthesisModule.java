package com.github.manebarros.core;

import java.util.List;

public interface SynthesisModule<E extends Execution> extends SynthesisProblemExtender {
  List<E> executions();
}
