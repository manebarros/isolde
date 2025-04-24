package haslab.isolde.core;

import kodkod.ast.Expression;
import kodkod.ast.Relation;

public final class DirectAbstractHistoryEncoding implements AbstractHistoryK {
  public static final Relation transactions = Relation.unary("transactions");
  public static final Relation keys = Relation.unary("keys");
  public static final Relation values = Relation.unary("values");
  public static final Relation sessions = Relation.unary("sessions");

  public static final Relation initialTransaction = Relation.unary("initialTransaction");

  public static final Relation reads = Relation.ternary("external reads");
  public static final Relation writes = Relation.ternary("final writes");

  public static final Relation sessionOrder = Relation.binary("session order");

  public static final Relation txn_session = Relation.binary("session function");

  private static DirectAbstractHistoryEncoding instance = null;

  private DirectAbstractHistoryEncoding() {}

  public static DirectAbstractHistoryEncoding instance() {
    if (instance == null) {
      instance = new DirectAbstractHistoryEncoding();
    }
    return instance;
  }

  @Override
  public Relation transactions() {
    return transactions;
  }

  @Override
  public Expression keys() {
    return keys;
  }

  @Override
  public Expression values() {
    return values;
  }

  @Override
  public Expression sessions() {
    return sessions;
  }

  @Override
  public Relation initialTransaction() {
    return initialTransaction;
  }

  @Override
  public Expression externalReads() {
    return reads;
  }

  @Override
  public Expression finalWrites() {
    return writes;
  }

  @Override
  public Expression sessionOrder() {
    return sessionOrder;
  }

  @Override
  public Expression txn_session() {
    return txn_session;
  }
}
