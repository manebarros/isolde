package haslab.isolde.history;

import java.util.Arrays;
import java.util.List;

public record AbstractSession(List<AbstractTransaction> transactions) {

  public AbstractSession(AbstractTransaction... ts) {
    this(Arrays.asList(ts));
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
