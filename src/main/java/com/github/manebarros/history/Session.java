package com.github.manebarros.history;

import java.util.Collections;
import java.util.List;

public record Session(List<Transaction> transactions) {

  public Session(Transaction t) {
    this(Collections.singletonList(t));
  }

  @Override
  public final String toString() {
    StringBuilder sb = new StringBuilder();
    if (!transactions().isEmpty()) {
      sb.append(transactions().get(0));

      for (var t : transactions().subList(1, transactions().size())) {
        sb.append("\n|\n").append(t);
      }
    } else {
      sb.append("*empty session*");
    }
    return sb.toString();
  }
}
