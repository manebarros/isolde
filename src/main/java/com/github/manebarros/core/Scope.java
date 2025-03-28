package com.github.manebarros.core;

public class Scope {
  private int transactions;
  private int objects;
  private int values;
  private int sessions;

  public Scope(int scope) {
    this(scope, scope, scope, scope);
  }

  public Scope(int transactions, int objects, int values, int sessions) {
    this.transactions = transactions;
    this.objects = objects;
    this.values = values;
    this.sessions = sessions;
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

  public int getSessions() {
    return sessions;
  }

  public void setSessions(int sessions) {
    this.sessions = sessions;
  }
}
