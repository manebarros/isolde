package com.github.manebarros.core.synth;

import java.util.Collection;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.TupleSet;

public interface SynthesisProblemExtender {
  Collection<Object> extraAtoms();

  Formula extend(Bounds b);

  Formula extend(Bounds b, TupleSet txnTotalOrderTs);
}
