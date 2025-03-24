package com.github.manebarros.cerone;

import com.github.manebarros.core.AbstractHistoryK;
import com.github.manebarros.core.Execution;
import kodkod.ast.Expression;

public record CeroneExecution(AbstractHistoryK history, Expression vis, Expression ar)
    implements Execution {}
