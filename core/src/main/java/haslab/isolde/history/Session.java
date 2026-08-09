package haslab.isolde.history;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A group of transactions that the session order relates, together with that order — held as pairs
 * of indices into {@code transactions}, the same way {@link AbstractHistory} holds it, so it does
 * not depend on transaction ids being unique.
 *
 * <p>Usually the order is a chain — a session in the ordinary sense — and that is how it prints.
 * The synthesis model does not force session order to decompose into chains, though: it constrains
 * it only to be transitive and within the transaction total order. A synthesized history can
 * therefore order two mutually incomparable transactions before a third, which no sequence of
 * transactions can express. Such a session prints its edges explicitly instead of pretending to be
 * a sequence.
 *
 * <p>{@code transactions} is always listed consistently with {@code order}, so a chain reads top to
 * bottom.
 */
public record Session(List<Transaction> transactions, Set<List<Integer>> order) {

  /** A session in the ordinary sense: every transaction precedes all the ones after it. */
  public Session(List<Transaction> transactions) {
    this(transactions, chainOver(transactions.size()));
  }

  public Session(Transaction... transactions) {
    this(Arrays.asList(transactions));
  }

  private static Set<List<Integer>> chainOver(int size) {
    Set<List<Integer>> order = new LinkedHashSet<>();
    for (int i = 0; i < size; i++) {
      for (int j = i + 1; j < size; j++) {
        order.add(List.of(i, j));
      }
    }
    return order;
  }

  /** Whether the order is total, i.e. these transactions really do form a sequence. */
  public boolean isChain() {
    for (int i = 0; i < transactions.size(); i++) {
      for (int j = i + 1; j < transactions.size(); j++) {
        if (!order.contains(List.of(i, j))) return false;
      }
    }
    return true;
  }

  /**
   * The edges not implied by transitivity — the ones worth printing. {@code order} relates every
   * pair it orders, so for a chain of n transactions this is the n-1 consecutive edges, which is
   * what makes a chain render as {@code 1 | 2 | 3} rather than as every pair.
   */
  public Set<List<Integer>> coveringEdges() {
    int n = transactions.size();
    Set<List<Integer>> covering = new LinkedHashSet<>();
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (!order.contains(List.of(i, j))) continue;
        boolean implied = false;
        for (int mid = 0; mid < n && !implied; mid++) {
          implied = order.contains(List.of(i, mid)) && order.contains(List.of(mid, j));
        }
        if (!implied) covering.add(List.of(i, j));
      }
    }
    return covering;
  }

  @Override
  public final String toString() {
    if (transactions.isEmpty()) return "*empty session*";

    if (isChain()) {
      return transactions.stream().map(Transaction::toString).collect(Collectors.joining("\n|\n"));
    }

    StringBuilder sb = new StringBuilder();
    for (var transaction : transactions) {
      sb.append(transaction).append("\n");
    }
    return sb.append("so: ")
        .append(
            coveringEdges().stream()
                .map(
                    e -> transactions.get(e.get(0)).id() + " -> " + transactions.get(e.get(1)).id())
                .collect(Collectors.joining(", ")))
        .toString();
  }
}
