package haslab.isolde.experiments.benchmark;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.SynthesizerI;
import haslab.isolde.core.synth.Scope;

public record IsoldeInput(
    Scope scope,
    IsoldeSpec problem,
    String problemName,
    SynthesizerI implementation,
    String implementationName,
    Solver solver) {

  public IsoldeInput(
      Scope scope, Named<IsoldeSpec> problem, Named<SynthesizerI> implementation, Solver solver) {
    this(
        scope,
        problem.value(),
        problem.name(),
        implementation.value(),
        implementation.name(),
        solver);
  }
}
