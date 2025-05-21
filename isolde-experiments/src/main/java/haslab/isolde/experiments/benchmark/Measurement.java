package haslab.isolde.experiments.benchmark;

import haslab.isolde.core.synth.Scope;
import java.lang.reflect.RecordComponent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public record Measurement(
    String implementation,
    String solver,
    String satisfied,
    String violated,
    Integer num_txn,
    Integer num_keys,
    Integer num_values,
    Integer num_sessions,
    Long time_ms,
    Integer candidates,
    Date run,
    Date date) {

  public Measurement(
      String implementation,
      String solver,
      String satisfied,
      String violated,
      Scope scope,
      Long time,
      Integer candidates,
      Date run,
      Date date) {
    this(
        implementation,
        solver,
        satisfied,
        violated,
        scope.getTransactions(),
        scope.getObjects(),
        scope.getValues(),
        scope.getSessions(),
        time,
        candidates,
        run,
        date);
  }

  public static String header() {
    StringBuilder sb = new StringBuilder();
    List<RecordComponent> components = Arrays.asList(Measurement.class.getRecordComponents());
    sb.append(components.get(0).getName());
    for (var component : components.subList(1, components.size())) {
      sb.append(",").append(component.getName());
    }
    return sb.toString();
  }

  public String asCsvRow() {
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    return String.format(
        "%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%s,%s",
        implementation,
        solver,
        satisfied,
        violated,
        num_txn,
        num_keys,
        num_values,
        num_sessions,
        time_ms,
        candidates,
        dateFormat.format(run),
        dateFormat.format(date));
  }
}
