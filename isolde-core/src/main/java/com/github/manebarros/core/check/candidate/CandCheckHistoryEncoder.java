package com.github.manebarros.core.check.candidate;

import com.github.manebarros.core.AbstractHistoryK;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public interface CandCheckHistoryEncoder {
  AbstractHistoryK encoding();

  Formula encode(Instance instance, AbstractHistoryK context, Bounds bounds);
}
