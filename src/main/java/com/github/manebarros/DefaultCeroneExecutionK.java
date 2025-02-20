package com.github.manebarros;

import kodkod.ast.Expression;

public record DefaultCeroneExecutionK(AbstractHistoryK history, Expression vis, Expression ar)
    implements CeroneExecutionK {}
