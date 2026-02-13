package haslab.isolde.history;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AbstractTransaction {
  private Map<Integer, Integer> reads;
  private Map<Integer, Integer> writes;

  public AbstractTransaction(Map<Integer, Integer> reads, Map<Integer, Integer> writes) {
    this.reads = reads;
    this.writes = writes;
  }

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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (var key : this.reads.keySet()) {
      sb.append(String.format("r(%d, %d) ", key, this.reads.get(key)));
    }
    for (var key : this.writes.keySet()) {
      sb.append(String.format("w(%d, %d) ", key, this.writes.get(key)));
    }
    sb.append('\n');
    return sb.toString();
  }

  public Set<Integer> keySet() {
    Set<Integer> r = new HashSet<>();
    r.addAll(reads.keySet());
    r.addAll(writes.keySet());
    return r;
  }

  public Set<Integer> valueSet() {
    Set<Integer> r = new HashSet<>();
    r.addAll(reads.values());
    r.addAll(writes.values());
    return r;
  }
}
