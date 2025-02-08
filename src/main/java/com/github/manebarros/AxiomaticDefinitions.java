package com.github.manebarros;

import static com.github.manebarros.HistoryEncoding.readsFrom;
import static com.github.manebarros.HistoryEncoding.value;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class AxiomaticDefinitions {
  private AxiomaticDefinitions() {}

  public static Formula cutIsolation(Expression commitOrder) {
    Variable x = Variable.unary("x");
    Variable t = Variable.unary("t");
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable r1 = Variable.unary("r1");
    Variable r2 = Variable.unary("r2");
    Variable w1 = Variable.unary("w1");
    Variable w2 = Variable.unary("w2");

    return Formula.and(t1.eq(t2).not(), r1.eq(r2).not(), readsFrom(w1, r1), readsFrom(w2, r2))
        .implies(value(r1).eq(value(r2)))
        .forAll(
            x.oneOf(HistoryEncoding.Object)
                .and(
                    t.oneOf(HistoryEncoding.readFromAnyOf(x))
                        .and(
                            t1.oneOf(HistoryEncoding.writeToAnyOf(x).difference(t))
                                .and(
                                    t2.oneOf(HistoryEncoding.writeToAnyOf(x).difference(t))
                                        .and(
                                            r1.oneOf(HistoryEncoding.readsOf(x, t))
                                                .and(
                                                    r2.oneOf(HistoryEncoding.readsOf(x, t))
                                                        .and(
                                                            w1.oneOf(
                                                                    HistoryEncoding.writesOf(x, t1))
                                                                .and(
                                                                    w2.oneOf(
                                                                        HistoryEncoding.writesOf(
                                                                            x, t2))))))))));
  }
}
