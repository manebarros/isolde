package haslab.isolde.experiments.benchmark;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.core.synth.Scope;
import kodkod.engine.config.Options;

/**
 * A benchmarkable synthesizer. Unlike {@code SynthesizerI}, this is the seam the harness measures
 * against: it reports a neutral {@link SynthesisOutcome} and can decline problems it does not handle
 * (e.g. the exhaustive enumerator is Biswas-only).
 */
public interface SynthesisRunner {
  String id();

  /** Whether this runner can handle the given problem. */
  boolean supports(IsoldeSpec spec);

  SynthesisOutcome run(Scope scope, IsoldeSpec spec, Options options);
}
