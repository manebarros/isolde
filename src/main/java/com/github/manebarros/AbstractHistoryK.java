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

  // TODO
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

  default Expression wr(Expression x) {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");

    return t.product(x)
        .product(s)
        .in(wr())
        .comprehension(t.oneOf(transactions()).and(s.oneOf(transactions())));
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
}
