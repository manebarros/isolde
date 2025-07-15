package haslab.isolde.core.synth.noSession;

import haslab.isolde.core.synth.Scope;

public class SimpleScope {
  private int transactions;
  private int objects;
  private int values;

  public SimpleScope(Scope scope) {
    this(scope.getTransactions(), scope.getObjects(), scope.getValues());
  }

  public SimpleScope(int transactions, int objects, int values) {
    this.transactions = transactions;
    this.objects = objects;
    this.values = values;
  }

  public int getTransactions() {
    return transactions;
  }

  public void setTransactions(int transactions) {
    this.transactions = transactions;
  }

  public int getObjects() {
    return objects;
  }

  public void setObjects(int objects) {
    this.objects = objects;
  }

  public int getValues() {
    return values;
  }

  public void setValues(int values) {
    this.values = values;
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
