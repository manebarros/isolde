package haslab.isolde.core.synth;

import haslab.isolde.core.Execution;

import java.util.List;

public interface SynthesisModule<E extends Execution> extends SynthesisProblemExtender {
  List<E> executions();
}
