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
}
