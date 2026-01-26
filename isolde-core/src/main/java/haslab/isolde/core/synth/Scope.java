package haslab.isolde.core.synth;

public class Scope {
  private final int transactions;
  private final int objects;
  private final int values;
  private final int sessions;

  private static final int DEFAULT_SCOPE = 3;

  public static class Builder {
    private int transactions;
    private int objects;
    private int values;
    private int sessions;

    public Builder(int scope) {
      this.transactions = scope;
      this.objects = scope;
      this.values = scope;
      this.sessions = scope;
    }

    public Builder() {
      this(DEFAULT_SCOPE);
    }

    public Builder txn(int scope) {
      transactions = scope;
      return this;
    }

    public Builder obj(int scope) {
      objects = scope;
      return this;
    }

    public Builder val(int scope) {
      values = scope;
      return this;
    }

    public Builder sess(int scope) {
      sessions = scope;
      return this;
    }

    public Scope build() {
      return new Scope(this);
    }
  }

  public Scope(int scope) {
    this(new Builder(scope));
  }

  private Scope(Builder builder) {
    this.transactions = builder.transactions;
    this.objects = builder.objects;
    this.values = builder.values;
    this.sessions = builder.sessions;
  }

  public int getTransactions() {
    return transactions;
  }

  public int getObjects() {
    return objects;
  }

  public int getValues() {
    return values;
  }

  public int getSessions() {
    return sessions;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.transactions).append(" transactions, ");
    sb.append(this.objects).append(" objects, ");
    sb.append(this.values).append(" values, ");
    sb.append(this.sessions).append(" sessions");
    return sb.toString();
  }
}
