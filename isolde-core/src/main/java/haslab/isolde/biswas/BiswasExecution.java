package haslab.isolde.biswas;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import kodkod.ast.Expression;

public record BiswasExecution(AbstractHistoryK history, Expression co) implements Execution {}
