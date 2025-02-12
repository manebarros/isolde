package com.github.manebarros;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public interface AbstractHistoryK {
  Expression transactions();

  Expression keys();

  Expression values();

  Expression sessions();

  Expression initialTransaction();

  Expression externalReads();

  Expression finalWrites();

  Expression sessionOrder();

  Expression txn_session();

  // TODO: We can try to use Kodkod's built in function concept to define wr
  default Expression wr() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    return finalWrite(t, x, n)
        .and(externalRead(s, x, n))
        .forSome(n.oneOf(values()))
        .comprehension(t.oneOf(transactions()).and(x.oneOf(keys())).and(s.oneOf(transactions())));
  }

  default Formula wr(Expression t, Expression x, Expression s) {
    return t.product(x).product(s).in(wr());
  }

  default Expression wr(Expression x) {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");

    return t.product(x)
        .product(s)
        .in(wr())
        .comprehension(t.oneOf(transactions()).and(s.oneOf(transactions())));
  }

  default Expression binaryWr() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    Variable x = Variable.unary("x");

    return t.product(x)
        .product(s)
        .in(wr())
        .forSome(x.oneOf(keys()))
        .comprehension(t.oneOf(transactions()).and(s.oneOf(transactions())));
  }

  default Expression causalOrder() {
    return binaryWr().union(sessionOrder()).closure();
  }

  default Formula causallyOrdered(Expression t, Expression s) {
    return s.in(t.join(causalOrder()));
  }

  default Formula sessionOrdered(Expression t, Expression s) {
    return s.in(t.join(sessionOrder()));
  }

  default Expression session(Expression t) {
    return t.join(txn_session());
  }

  default Formula noReadsFromThinAir() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    Variable x = Variable.unary("x");
    return reads(s, x)
        .implies(t.product(x).product(s).in(wr()).forSome(t.oneOf(transactions())))
        .forAll(s.oneOf(normalTxns()).and(x.oneOf(keys())));
  }

  default Formula finalWrite(Expression t, Expression x, Expression n) {
    return t.product(x).product(n).in(finalWrites());
  }

  default Formula externalRead(Expression t, Expression x, Expression n) {
    return t.product(x).product(n).in(externalReads());
  }

  default Formula writes(Expression t, Expression x) {
    return x.join(t.join(finalWrites())).some();
  }

  default Formula reads(Expression t, Expression x) {
    return x.join(t.join(externalReads())).some();
  }

  default Expression normalTxns() {
    return transactions().difference(initialTransaction());
  }

  default Expression txnThatWriteToAnyOf(Expression x) {
    return finalWrites().join(values()).join(x);
  }

  default Expression txnThatReadAnyOf(Expression x) {
    return externalReads().join(values()).join(x);
  }

  default Expression readSet(Expression t) {
    return t.join(externalReads()).join(values());
  }

  default Expression writeSet(Expression t) {
    return t.join(finalWrites()).join(values());
  }
}
