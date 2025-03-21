package com.github.manebarros.generic;

import java.util.Collection;
import java.util.List;

public record SynthesisModule<E extends Execution>(
    List<E> executions, Collection<Object> atoms, ProblemExtender extender) {}
