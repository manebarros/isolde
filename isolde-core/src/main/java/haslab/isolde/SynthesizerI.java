package haslab.isolde;

import haslab.isolde.core.synth.Scope;
import kodkod.engine.config.Options;

public interface SynthesizerI {
  SynthesizedHistory synthesize(Scope scope, IsoldeSpec spec);

  SynthesizedHistory synthesize(
      Scope scope, IsoldeSpec spec, Options checkingOptions, Options synthOptions);

  default SynthesizedHistory synthesize(Scope scope, IsoldeSpec spec, Options options) {
    return synthesize(scope, spec, options, options);
  }
}
