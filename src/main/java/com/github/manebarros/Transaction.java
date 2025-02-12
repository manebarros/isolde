package com.github.manebarros;

import java.util.List;

public record Transaction(int id, List<Operation> operations) {
  @Override
  public final String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(id + ": ");
    for (var op : this.operations()) {
      sb.append(op).append(" ");
    }
    return sb.toString();
  }
}
