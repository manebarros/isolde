package com.github.manebarros.kodkod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

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

  public static Expression max(Expression r, Expression s) {
    Variable u = Variable.unary("u");
    Variable v = Variable.unary("v");

    Formula f = v.eq(u).or(v.product(u).in(r)).forAll(v.oneOf(s));

    return f.comprehension(u.oneOf(s));
  }

  public static Expression min(Expression r, Expression s) {
    Variable u = Variable.unary("u");
    Variable v = Variable.unary("v");

    Formula f = v.eq(u).or(u.product(v).in(r)).forAll(v.oneOf(s));

    return f.comprehension(u.oneOf(s));
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

  public static Formula strictPartialOrder(Expression r, Expression s) {
    return Formula.and(irreflexive(r), asymmetric(r, s), transitive(r));
  }

  public static Formula irreflexive(Expression r) {
    return r.intersection(Expression.IDEN).no();
  }

  public static Formula asymmetric(Expression r, Expression s) {
    Variable a = Variable.unary("a");
    Variable b = Variable.unary("b");

    return a.product(b).in(r).implies(b.product(a).in(r).not()).forAll(a.oneOf(s).and(b.oneOf(s)));
  }

  public static Formula acyclic(Expression r) {
    return r.closure().intersection(Expression.IDEN).no();
  }

  public static Formula function(Expression f, Expression domain) {
    Variable x = Variable.unary("x");
    return x.join(f).one().forAll(x.oneOf(domain));
  }

  public static Formula strictTotalOrder(Expression r, Expression s) {
    return strictPartialOrder(r, s).and(total(r, s));
  }

  public static Formula disj(Collection<? extends Expression> expressions) {
    Formula formula = Formula.TRUE;
    List<Expression> l = new ArrayList<>(expressions);
    for (int i = 1; i < l.size(); i++) {
      for (int j = 0; j < i; j++) {
        formula = formula.and(l.get(i).eq(l.get(j)).not());
      }
    }
    return formula;
  }

  public static TupleSet asTupleSet(TupleFactory f, Collection<?> atoms) {
    return f.setOf(atoms.stream().map(a -> f.tuple(a)).collect(Collectors.toList()));
  }
}
