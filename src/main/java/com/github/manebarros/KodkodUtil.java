package com.github.manebarros;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class KodkodUtil {
  private KodkodUtil() {}

  public static Expression transitiveReduction(Expression exp) {
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");

    return x.product(y)
        .in(exp)
        .and(x.product(y).in(exp.difference(x.product(y)).closure()).not())
        .comprehension(x.oneOf(Expression.UNIV).and(y.oneOf(Expression.UNIV)));
  }

  public static Formula min(Expression x, Expression rel, Expression set) {
    Variable y = Variable.unary("y");
    return y.eq(x).or(x.product(y).in(rel)).forAll(y.oneOf(set));
  }

  public static Formula max(Expression x, Expression rel, Expression set) {
    Variable y = Variable.unary("y");
    return y.eq(x).or(y.product(x).in(rel)).forAll(y.oneOf(set));
  }

  public static Formula total(Expression relation, Expression set) {
    Variable x = Variable.unary("x");
    Variable y = Variable.unary("y");
    return x.eq(y)
        .not()
        .implies(x.product(y).in(relation).or(y.product(x).in(relation)))
        .forAll(x.oneOf(set).and(y.oneOf(set)));
  }

  public static Formula transitive(Expression relation) {
    return relation.join(relation).in(relation);
  }
}
