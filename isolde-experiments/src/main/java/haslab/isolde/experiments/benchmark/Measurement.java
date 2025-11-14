package haslab.isolde.experiments.benchmark;

import haslab.isolde.core.cegis.CegisResult;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public record Measurement(
    IsoldeInput input, long time, boolean timedOut, int candidates, Date runId, Date endTime) {

  public Measurement(IsoldeInput input, CegisResult result, Date runId, Date startTime) {
    this(input, result.getTime(), false, result.candidatesNr(), runId, startTime);
  }

  public Measurement(
      IsoldeInput input, CegisResult result, boolean timedOut, Date runId, Date startTime) {
    this(input, result.getTime(), timedOut, result.candidatesNr(), runId, startTime);
  }

  public static Measurement timeout(IsoldeInput input, long time_ms, Date runId, Date startTime) {
    return new Measurement(input, time_ms, true, -1, runId, startTime);
  }

  public static String header() {
    List<String> columns = new ArrayList<>();
    columns.add("implementation");
    columns.add("solver");
    columns.add("problem");
    columns.add("num_txn");
    columns.add("num_keys");
    columns.add("num_values");
    columns.add("num_sessions");
    columns.add("time_ms");
    columns.add("timed_out");
    columns.add("candidates");
    columns.add("runId");
    columns.add("endTime");
    StringBuilder sb = new StringBuilder();
    sb.append(columns.get(0));
    for (int i = 1; i < columns.size(); i++) {
      sb.append("," + columns.get(i));
    }
    return sb.toString();
  }

  public String asCsvRow() {
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    return String.format(
        "%s,%s,%s,%d,%d,%d,%d,%d,%s,%d,%s,%s",
        input.implementationName(),
        input.solver(),
        input.problemName(),
        input.scope().getTransactions(),
        input.scope().getObjects(),
        input.scope().getValues(),
        input.scope().getSessions(),
        time,
        timedOut,
        candidates,
        dateFormat.format(runId),
        dateFormat.format(endTime));
  }

  public static String asCsv(Collection<Measurement> measurements) {
    return Measurement.header() + "\n" + asCsvWithoutHeader(measurements);
  }

  public static String asCsvWithoutHeader(Collection<Measurement> measurements) {
    return Util.unlines(
        measurements.stream().map(Measurement::asCsvRow).collect(Collectors.toList()));
  }
}
