package com.github.manebarros.generic;

import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.TupleSet;

public interface ProblemExtender {
  Formula extend(Bounds b);

  Formula extend(Bounds b, TupleSet txnTotalOrderTs);
}
