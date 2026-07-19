package haslab.isolde.experiments.benchmark;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.core.synth.Scope;

public record IsoldeInput(
    Scope scope,
    IsoldeSpec problem,
    String problemName,
    SynthesisRunner implementation,
    String implementationName,
    Solver solver) {

  public IsoldeInput(
      Scope scope, Named<IsoldeSpec> problem, Named<SynthesisRunner> implementation, Solver solver) {
    this(
        scope,
        problem.value(),
        problem.name(),
        implementation.value(),
        implementation.name(),
        solver);
  }
}
