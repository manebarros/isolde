package haslab.isolde;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;

public record NamedConstraint<E extends Execution>(String name, ExecutionFormula<E> constraint) {}
