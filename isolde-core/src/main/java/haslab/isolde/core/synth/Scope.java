package haslab.isolde.core.synth;

public class Scope {
  private final int transactions;
  private final int objects;
  private final int values;

  private static final int DEFAULT_SCOPE = 3;

  public static class Builder {
    private int transactions;
    private int objects;
    private int values;

    public Builder(int scope) {
      this.transactions = scope;
      this.objects = scope;
      this.values = scope;
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.transactions).append(" transactions, ");
    sb.append(this.objects).append(" objects, ");
    sb.append(this.values).append(" values, ");
    return sb.toString();
  }
}
