package haslab.isolde.experiments.benchmark;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Measurement(
    IsoldeInput input, long totalTime, Outcome outcome, Date runId, Date endTime) {

  /** What became of a run: it completed with a verdict, hit the timeout, or crashed. */
  public sealed interface Outcome {

    /** A run that reached a verdict, satisfiable or not. */
    record Completed(
        boolean sat,
        long synthTime,
        long checkTime,
        int initialSynthClauses,
        int totalSynthClauses,
        int candidates)
        implements Outcome {}

    /** A run stopped by the benchmark timeout. */
    record TimedOut() implements Outcome {}

    /** A run aborted by an error; in practice, an {@link OutOfMemoryError}. */
    record Crashed(Throwable cause) implements Outcome {}

    /** The label written to the {@code outcome} CSV column. */
    default String label() {
      return switch (this) {
        case Completed c -> c.sat() ? "SAT" : "UNSAT";
        case TimedOut() -> "TIMEOUT";
        case Crashed c -> "CRASH";
      };
    }
  }

  public static Measurement finished(
      IsoldeInput input, SynthesisOutcome outcome, Date runId, Date endTime) {
    return new Measurement(
        input,
        outcome.totalTimeMillis(),
        new Outcome.Completed(
            outcome.sat(),
            outcome.synthTimeMillis(),
            outcome.checkTimeMillis(),
            outcome.initialSynthClauses(),
            outcome.finalSynthClauses(),
            outcome.candidates()),
        runId,
        endTime);
  }

  public static Measurement timeout(IsoldeInput input, long time_ms, Date runId, Date endTime) {
    return new Measurement(input, time_ms, new Outcome.TimedOut(), runId, endTime);
  }

  public static Measurement crash(
      IsoldeInput input, Throwable cause, long time_ms, Date runId, Date endTime) {
    return new Measurement(input, time_ms, new Outcome.Crashed(cause), runId, endTime);
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
          new Column("synth_time_ms", ifCompleted(c -> Long.toString(c.synthTime()))),
          new Column("check_time_ms", ifCompleted(c -> Long.toString(c.checkTime()))),
          new Column("total_time_ms", m -> Long.toString(m.totalTime())),
          new Column(
              "initial_synth_clauses", ifCompleted(c -> Integer.toString(c.initialSynthClauses()))),
          new Column(
              "total_synth_clauses", ifCompleted(c -> Integer.toString(c.totalSynthClauses()))),
          new Column("outcome", m -> m.outcome().label()),
          new Column("candidates", ifCompleted(c -> Integer.toString(c.candidates()))),
          new Column("runId", m -> formatDate(m.runId())),
          new Column("endTime", m -> formatDate(m.endTime())));

  /** A cell that only completed runs can fill; timed out and crashed runs leave it empty. */
  private static Function<Measurement, String> ifCompleted(
      Function<Outcome.Completed, String> value) {
    return m -> m.outcome() instanceof Outcome.Completed c ? value.apply(c) : "";
  }

  public static String header() {
    return COLUMNS.stream().map(Column::name).collect(Collectors.joining(","));
  }

  public String asCsvRow() {
    return COLUMNS.stream().map(c -> c.value().apply(this)).collect(Collectors.joining(","));
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
