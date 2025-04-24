package haslab.isolde.cerone;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import kodkod.ast.Expression;

public record CeroneExecution(AbstractHistoryK history, Expression vis, Expression ar)
    implements Execution {}
