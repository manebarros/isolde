package haslab.isolde.core;

import kodkod.ast.Relation;

public final class DirectAbstractHistoryEncoding implements AbstractHistoryRel {

  private static final Relation TRANSACTIONS = Relation.unary("transactions");
  private static final Relation KEYS = Relation.unary("keys");
  private static final Relation VALUES = Relation.unary("values");
  private static final Relation INITIAL_TRANSACTION = Relation.unary("initialTransaction");
  private static final Relation READS = Relation.ternary("external reads");
  private static final Relation WRITES = Relation.ternary("final writes");
  private static final Relation SESSION_ORDER = Relation.binary("session order");

  public static final DirectAbstractHistoryEncoding INSTANCE = new DirectAbstractHistoryEncoding();

  private DirectAbstractHistoryEncoding() {}

  @Override
  public Relation transactions() {
    return TRANSACTIONS;
  }

  @Override
  public Relation keys() {
    return KEYS;
  }

  @Override
  public Relation values() {
    return VALUES;
  }

  @Override
  public Relation initialTransaction() {
    return INITIAL_TRANSACTION;
  }

  @Override
  public Relation externalReads() {
    return READS;
  }

  @Override
  public Relation finalWrites() {
    return WRITES;
  }

  @Override
  public Relation sessionOrder() {
    return SESSION_ORDER;
  }
}
