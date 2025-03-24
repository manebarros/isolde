package com.github.manebarros.history;

import java.util.HashMap;
import java.util.Map;

public class AbstractTransaction {
  private Map<Integer, Integer> reads;
  private Map<Integer, Integer> writes;

  public AbstractTransaction(Transaction t) throws TransactionNotInternallyConsistentException {
    Map<Integer, Integer> state = new HashMap<>();
    this.reads = new HashMap<>();
    this.writes = new HashMap<>();
    for (var op : t.operations()) {
      var key = op.object();
      var val = op.value();
      if (op.isWrite()) {
        state.put(key, val);
        writes.put(key, val);
      } else if (!state.containsKey(key)) {
        state.put(key, val);
        reads.put(key, val);
      } else if (val != state.get(key)) {
        throw new TransactionNotInternallyConsistentException(t, key, state.get(key), val);
      }
    }
  }

  public Map<Integer, Integer> getReads() {
    return reads;
  }

  public Map<Integer, Integer> getWrites() {
    return writes;
  }
}
