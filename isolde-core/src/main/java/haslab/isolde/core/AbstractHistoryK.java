package haslab.isolde.core;

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

  default Expression writerOf(Expression x, Expression v) {
    return finalWrites().join(v).join(x);
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

  default Expression txnThatTouchAnyOf(Expression x) {
    return finalWrites().union(externalReads()).join(values()).join(x);
  }

  default Formula isUpdateTransaction(Expression t) {
    return t.join(finalWrites()).some();
  }

  default Expression readSet(Expression t) {
    return t.join(externalReads()).join(values());
  }

  default Expression writeSet(Expression t) {
    return t.join(finalWrites()).join(values());
  }

  default Expression mandatoryCommitOrderEdgesCC() {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return causallyOrdered(t2, t1)
        .or(
            Formula.and(t1.eq(t2).not(), writes(t2, x), wr(t1, x, t3), causallyOrdered(t2, t3))
                .forSome(x.oneOf(keys()).and(t3.oneOf(transactions().difference(t1.union(t2))))))
        .comprehension(t2.oneOf(transactions()).and(t1.oneOf(transactions())))
        .closure();
  }

  default Expression mandatoryCommitOrderEdgesRA() {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return causallyOrdered(t2, t1)
        .or(
            Formula.and(
                    t1.eq(t2).not(),
                    writes(t2, x),
                    wr(t1, x, t3),
                    t2.product(t3).in(sessionOrder().union(binaryWr())))
                .forSome(x.oneOf(keys()).and(t3.oneOf(transactions().difference(t1.union(t2))))))
        .comprehension(t2.oneOf(transactions()).and(t1.oneOf(transactions())))
        .closure();
  }

  default Expression initialValue(Expression key) {
    return key.join(initialTransaction().join(finalWrites()));
  }

  default Expression valuesWrittenTo(Expression x) {
    return x.join(transactions().join(finalWrites()));
  }

  default AbstractHistoryK projectionOverKeys(Expression keySubset) {
    AbstractHistoryK og = this;

    return new AbstractHistoryK() {

      @Override
      public Expression transactions() {
        return og.txnThatTouchAnyOf(keySubset);
      }

      @Override
      public Expression keys() {
        return og.keys();
      }

      @Override
      public Expression values() {
        return og.values();
      }

      @Override
      public Expression sessions() {
        return og.sessions();
      }

      @Override
      public Expression initialTransaction() {
        return og.initialTransaction();
      }

      @Override
      public Expression externalReads() {
        return og.externalReads().intersection(transactions().product(keySubset).product(values()));
      }

      @Override
      public Expression finalWrites() {
        return og.finalWrites().intersection(transactions().product(keySubset).product(values()));
      }

      @Override
      public Expression sessionOrder() {
        return og.sessionOrder();
      }

      @Override
      public Expression txn_session() {
        return og.txn_session();
      }
    };
  }

  default AbstractHistoryK subHistory(Expression txnSubset) {
    AbstractHistoryK og = this;

    return new AbstractHistoryK() {

      @Override
      public Expression transactions() {
        return initialTransaction().union(txnSubset);
      }

      @Override
      public Expression keys() {
        return og.keys();
      }

      @Override
      public Expression values() {
        return og.values();
      }

      @Override
      public Expression sessions() {
        return og.sessions();
      }

      @Override
      public Expression initialTransaction() {
        return og.initialTransaction();
      }

      @Override
      public Expression externalReads() {
        return og.externalReads();
      }

      @Override
      public Expression finalWrites() {
        return og.finalWrites();
      }

      @Override
      public Expression sessionOrder() {
        return og.sessionOrder();
      }

      @Override
      public Expression txn_session() {
        return og.txn_session();
      }
    };
  }
}
