package haslab.isolde.history;

import java.util.List;
import java.util.Set;

public class AbstractHistory {
  private final List<AbstractTransaction> transactions;
  private final Set<List<Integer>> sessionOrder;

  public AbstractHistory(List<AbstractTransaction> transactions, Set<List<Integer>> sessionOrder) {
    this.transactions = transactions;
    this.sessionOrder = sessionOrder;
  }

  public List<AbstractTransaction> getTransactions() {
    return transactions;
  }

  public Set<List<Integer>> getSessionOrder() {
    return sessionOrder;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < transactions.size(); i++) {
      sb.append(String.format("T%d : %s", i, transactions.get(i)));
    }
    sb.append('\n').append("so: ").append(sessionOrder);
    return sb.toString();
  }
}
