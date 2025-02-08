package com.github.manebarros;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;

public final class HistoryEncoding {

  private HistoryEncoding() {}

  public static final Relation Transaction = Relation.unary("Transaction");
  public static final Relation Event = Relation.unary("Event");
  public static final Relation Object = Relation.unary("Object");
  public static final Relation Value = Relation.unary("Value");
  public static final Relation Session = Relation.unary("Session");

  public static final Relation Write = Relation.unary("Write");
  public static final Relation Read = Relation.unary("Read");

  public static final Relation InitialValue = Relation.unary("InitialValue");

  public static final Relation events = Relation.binary("events");
  public static final Relation object = Relation.binary("object");
  public static final Relation value = Relation.binary("value");
  public static final Relation programOrder = Relation.binary("programOrder");
  public static final Relation sessionOrder = Relation.binary("sessionOrder");
  public static final Relation session = Relation.binary("session");

  public static Expression causaOrder() {
    return sessionOrder.union(binary_wr()).closure();
  }

  public static Expression binary_wr() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    return t.join(HistoryEncoding.wr())
        .join(s)
        .some()
        .comprehension(t.oneOf(Transaction).and(s.oneOf(Transaction)));
  }

  public static Expression wr() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    Variable x = Variable.unary("x");
    Variable v = Variable.unary("v");
    return Formula.and(t.eq(s).not(), finalWrite(t, x, v), externalRead(s, x, v))
        .forSome(v.oneOf(Value))
        .comprehension(t.oneOf(Transaction).and(x.oneOf(Object).and(s.oneOf(Transaction))));
  }

  public static Expression writes(Expression t) {
    return t.join(events).intersection(Write);
  }

  public static Expression reads(Expression t) {
    return t.join(events).intersection(Read);
  }

  public static Formula writes(Expression t, Expression x) {
    return x.in(writes(t).join(object));
  }

  public static Formula reads(Expression t, Expression x) {
    return x.in(reads(t).join(object));
  }

  public static Expression writesOf(Expression x, Expression t) {
    return t.join(events).intersection(Write).intersection(object.join(x));
  }

  public static Expression readsOf(Expression x, Expression t) {
    return t.join(events).intersection(Write).intersection(object.join(x));
  }

  public static Formula externalRead(Expression t, Expression x, Expression v) {
    Variable e = Variable.unary("e");
    Variable f = Variable.unary("f");
    return Formula.and(
            e.join(object).eq(x),
            e.join(value).eq(v),
            Formula.or(f.eq(e), e.product(f).in(programOrder), f.join(object).eq(x).not())
                .forAll(f.oneOf(reads(t))))
        .forSome(e.oneOf(reads(t)));
  }

  public static Formula finalWrite(Expression t, Expression x, Expression v) {
    Variable e = Variable.unary("e");
    Variable f = Variable.unary("f");
    return Formula.and(
            e.join(object).eq(x),
            e.join(value).eq(v),
            Formula.or(f.eq(e), f.product(e).in(programOrder), f.join(object).eq(x).not())
                .forAll(f.oneOf(writes(t))))
        .forSome(e.oneOf(writes(t)));
  }

  public static Expression readFromAnyOf(Expression items) {
    return events.join(object.join(items).intersection(Read));
  }

  public static Expression writeToAnyOf(Expression items) {
    return events.join(object.join(items).intersection(Write));
  }

  public static Formula readsFrom(Expression w, Expression r) {
    return w.join(object).eq(r.join(object)).and(w.join(value).eq(r.join(value)));
  }

  public static Expression value(Expression op) {
    return op.join(value);
  }

  public static Expression object(Expression op) {
    return op.join(object);
  }
}
