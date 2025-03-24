package com.github.manebarros.biswas;

import com.github.manebarros.core.AbstractHistoryK;
import com.github.manebarros.core.Execution;
import kodkod.ast.Expression;

public record BiswasExecution(AbstractHistoryK history, Expression co) implements Execution {}
