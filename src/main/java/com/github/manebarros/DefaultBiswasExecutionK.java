package com.github.manebarros;

import kodkod.ast.Expression;

public record DefaultBiswasExecutionK(AbstractHistoryK history, Expression commitOrder)
    implements BiswasExecutionK {}
