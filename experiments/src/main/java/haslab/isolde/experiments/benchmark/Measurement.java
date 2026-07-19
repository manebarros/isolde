package haslab.isolde.experiments.benchmark;

import haslab.isolde.core.cegis.CegisResult;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
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
    TIMEOUT,
    CRASH,
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

  public static Measurement crash(IsoldeInput input, long time_ms, Date runId, Date startTime) {
    return new Measurement(input, -1, -1, time_ms, -1, -1, Outcome.CRASH, -1, runId, startTime);
  }

  private record Column(String name, Function<Measurement, String> value) {}

  private static final List<Column> COLUMNS =
      List.of(
          new Column("implementation", m -> m.input().implementationName()),
          new Column("solver", m -> m.input().solver().getId()),
          new Column("problem", m -> m.input().problemName()),
          new Column("num_txn", m -> Integer.toString(m.input().scope().getTransactions())),
          new Column("num_keys", m -> Integer.toString(m.input().scope().getObjects())),
          new Column("num_values", m -> Integer.toString(m.input().scope().getValues())),
          new Column("synth_time_ms", m -> num(m.synthTime())),
          new Column("check_time_ms", m -> num(m.checkTime())),
          new Column("total_time_ms", m -> num(m.totalTime())),
          new Column("initial_synth_clauses", m -> num(m.initialSynthClauses())),
          new Column("total_synth_clauses", m -> num(m.totalSynthClauses())),
          new Column("outcome", m -> m.outcome().toString()),
          new Column("candidates", m -> num(m.candidates())),
          new Column("runId", m -> formatDate(m.runId())),
          new Column("endTime", m -> formatDate(m.endTime())));

  public static String header() {
    return COLUMNS.stream().map(Column::name).collect(Collectors.joining(","));
  }

  public String asCsvRow() {
    return COLUMNS.stream().map(c -> c.value().apply(this)).collect(Collectors.joining(","));
  }

  /** Renders a numeric cell, using an empty cell for the -1 "not applicable" sentinel. */
  private static String num(long value) {
    return value < 0 ? "" : Long.toString(value);
  }

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

  private static String formatDate(Date date) {
    return DATE_FORMAT.format(date.toInstant());
  }

  public static String asCsv(Collection<Measurement> measurements) {
    return Measurement.header() + "\n" + asCsvWithoutHeader(measurements);
  }

  public static String asCsvWithoutHeader(Collection<Measurement> measurements) {
    return Util.unlines(
        measurements.stream().map(Measurement::asCsvRow).collect(Collectors.toList()));
  }
}
