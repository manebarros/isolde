package com.github.manebarros;

import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public record KodkodProblem(Formula formula, Bounds bounds) {}
