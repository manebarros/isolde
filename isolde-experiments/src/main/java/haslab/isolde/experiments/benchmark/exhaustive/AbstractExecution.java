package haslab.isolde.experiments.benchmark.exhaustive;

import haslab.isolde.history.AbstractHistory;
import haslab.isolde.history.AbstractTransaction;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AbstractExecution {
  private final List<AbstractTransaction> transactions;
  private final Set<List<Integer>> sessionOrder;
  private final List<Integer> commitOrder;

  public AbstractExecution(AbstractHistory history, List<Integer> commitOrder) {
    this.transactions = history.getTransactions();
    this.sessionOrder = history.getSessionOrder();
    this.commitOrder = commitOrder;
  }

  public AbstractExecution(
      List<AbstractTransaction> transactions,
      Set<List<Integer>> sessionOrder,
      List<Integer> commitOrder) {
    this.transactions = transactions;
    this.sessionOrder = sessionOrder;
    this.commitOrder = commitOrder;
  }

  public List<AbstractTransaction> getTransactions() {
    return transactions;
  }

  public Set<List<Integer>> getSessionOrder() {
    return sessionOrder;
  }

  public List<Integer> getCommitOrder() {
    return commitOrder;
  }

  public Set<Integer> keySet() {
    return this.transactions.stream()
        .map(AbstractTransaction::keySet)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  public Set<Integer> valueSet() {
    return this.transactions.stream()
        .map(AbstractTransaction::valueSet)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < transactions.size(); i++) {
      sb.append(String.format("T%d : %s", i, transactions.get(i)));
    }
    sb.append('\n')
        .append("so: ")
        .append(sessionOrder)
        .append("\n")
        .append("co: ")
        .append(commitOrder)
        .append('\n');
    return sb.toString();
  }
}
