package haslab.isolde.experiments.benchmark;

import haslab.isolde.core.cegis.CegisResult;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public record Measurement(
    IsoldeInput input,
    long synthTime,
    long checkTime,
    long totalTime,
    int initialSynthClauses,
    int totalSynthClauses,
    Outcome outcome,
    int candidates,
    Date runId,
    Date endTime) {

  public static enum Outcome {
    SAT,
    UNSAT,
    TIMEOUT
  }

  public static Measurement finished(
      IsoldeInput input, CegisResult result, Date runId, Date startTime) {
    return new Measurement(
        input,
        result.getSynthTime(),
        result.getCheckTime(),
        result.getTotalTime(),
        result.getInitialSynthClauses(),
        result.getFinalSynthClauses(),
        result.sat() ? Outcome.SAT : Outcome.UNSAT,
        result.candidatesNr(),
        runId,
        startTime);
  }

  public static Measurement timeout(IsoldeInput input, long time_ms, Date runId, Date startTime) {
    return new Measurement(input, -1, -1, time_ms, -1, -1, Outcome.TIMEOUT, -1, runId, startTime);
  }

  public static String header() {
    List<String> columns =
        Arrays.asList(
            "implementation",
            "solver",
            "problem",
            "num_txn",
            "num_keys",
            "num_values",
            "synth_time_ms",
            "check_time_ms",
            "total_time_ms",
            "initial_synth_clauses",
            "total_synth_clauses",
            "outcome",
            "candidates",
            "runId",
            "endTime");
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
        "%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%s,%d,%s,%s",
        input.implementationName(),
        input.solver(),
        input.problemName(),
        input.scope().getTransactions(),
        input.scope().getObjects(),
        input.scope().getValues(),
        synthTime,
        checkTime,
        totalTime,
        initialSynthClauses,
        totalSynthClauses,
        outcome,
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
