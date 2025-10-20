package haslab.isolde.experiments.benchmark;

import haslab.isolde.core.cegis.CegisResult;
import haslab.isolde.core.synth.Scope;
import java.lang.reflect.RecordComponent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public record SimpleMeasurement(
    String implementation,
    String solver,
    String problem,
    Integer num_txn,
    Integer num_keys,
    Integer num_values,
    Integer num_sessions,
    Long time_ms,
    Integer candidates,
    Date run,
    Date date) {

  public SimpleMeasurement(
      String implementation,
      String solver,
      String problem,
      Scope scope,
      Long time,
      Integer candidates,
      Date run,
      Date date) {
    this(
        implementation,
        solver,
        problem,
        scope.getTransactions(),
        scope.getObjects(),
        scope.getValues(),
        scope.getSessions(),
        time,
        candidates,
        run,
        date);
  }

  public SimpleMeasurement(
      String implementation,
      String solver,
      String problem,
      Scope scope,
      CegisResult result,
      Date run,
      Date date) {
    this(
        implementation,
        solver,
        problem,
        scope.getTransactions(),
        scope.getObjects(),
        scope.getValues(),
        scope.getSessions(),
        result.getTime(),
        result.candidatesNr(),
        run,
        date);
  }

  public static String header() {
    StringBuilder sb = new StringBuilder();
    List<RecordComponent> components = Arrays.asList(SimpleMeasurement.class.getRecordComponents());
    sb.append(components.get(0).getName());
    for (var component : components.subList(1, components.size())) {
      sb.append(",").append(component.getName());
    }
    return sb.toString();
  }

  public String asCsvRow() {
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    return String.format(
        "%s,%s,%s,%d,%d,%d,%d,%d,%d,%s,%s",
        implementation,
        solver,
        problem,
        num_txn,
        num_keys,
        num_values,
        num_sessions,
        time_ms,
        candidates,
        dateFormat.format(run),
        dateFormat.format(date));
  }

  public static String asCsv(Collection<SimpleMeasurement> measurements) {
    return SimpleMeasurement.header() + "\n" + asCsvWithoutHeader(measurements);
  }

  public static String asCsvWithoutHeader(Collection<SimpleMeasurement> measurements) {
    return Util.unlines(
        measurements.stream().map(SimpleMeasurement::asCsvRow).collect(Collectors.toList()));
  }
}
