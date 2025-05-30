package haslab.isolde;

import haslab.isolde.core.Execution;

public record NamedExecution<E extends Execution>(String name, E execution) {}
