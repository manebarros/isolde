package haslab.isolde.experiments.benchmark;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.IsoldeSynthesizer;
import haslab.isolde.core.synth.Scope;

public record IsoldeInput(
    Scope scope,
    IsoldeSpec problem,
    String problemName,
    IsoldeSynthesizer implementation,
    String implementationName,
    String solver) {

  public IsoldeInput(
      Scope scope,
      Named<IsoldeSpec> problem,
      Named<IsoldeSynthesizer> implementation,
      String solver) {
    this(
        scope,
        problem.value(),
        problem.name(),
        implementation.value(),
        implementation.name(),
        solver);
  }
}
