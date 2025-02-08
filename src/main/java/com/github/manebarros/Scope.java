package com.github.manebarros;

public class Scope {
  private int transactions;
  private int operations;
  private int objects;
  private int values;
  private int sessions;

  public Scope(int transactions, int operations, int objects, int values, int sessions) {
    this.transactions = transactions;
    this.operations = operations;
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

  public int getOperations() {
    return operations;
  }

  public void setOperations(int operations) {
    this.operations = operations;
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
