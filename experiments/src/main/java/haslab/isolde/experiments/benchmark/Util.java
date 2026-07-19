package haslab.isolde.experiments.benchmark;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.WRITE;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.SynthesizedHistory;
import haslab.isolde.SynthesizerI;
import haslab.isolde.core.synth.Scope;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import kodkod.engine.config.Options;

public final class Util {

  private static void writeString(String s, Path p) throws IOException {
    Path dir = p.getParent();
    if (!Files.exists(dir)) Files.createDirectories(dir);
    Files.writeString(p, s);
  }

  private static void appendString(String s, Path p) throws IOException {
    assert Files.exists(p);
    Files.writeString(p, s, APPEND, WRITE);
  }

  public static void writeMeasurements(List<Measurement> measurements, Path p) throws IOException {
    writeString(Measurement.asCsv(measurements), p);
  }

  public static void appendMeasurements(List<Measurement> measurements, Path p) throws IOException {
    if (Files.exists(p)) {
      appendString(Measurement.asCsvWithoutHeader(measurements), p);
    } else {
      writeMeasurements(measurements, p);
    }
  }

  public static <R> String unlines(List<R> rows) {
    StringBuilder sb = new StringBuilder();
    for (var row : rows) sb.append(row).append("\n");
    return sb.toString();
  }

  public static List<Scope> scopesFromRange(Range txn, Range objects, Range values) {
    List<Scope> scopes = new ArrayList<>();
    for (int txn_num = txn.min(); txn_num <= txn.max(); txn_num += 1) {
      for (int obj_num = objects.min(); obj_num <= objects.max(); obj_num += 1) {
        for (int val_num = values.min(); val_num <= values.max(); val_num += 1) {
          scopes.add(new Scope.Builder().txn(txn_num).obj(obj_num).val(val_num).build());
        }
      }
    }
    return scopes;
  }

  public static List<Scope> scopesFromRange(int keys, int val, int from, int to) {
    return scopesFromRange(keys, val, from, to, 1);
  }

  public static List<Scope> scopesFromRange(int keys, int val, int from, int to, int step) {
    List<Scope> scopes = new ArrayList<>();
    for (int txn_num = from; txn_num <= to; txn_num += step) {
      scopes.add(new Scope.Builder().txn(txn_num).obj(keys).val(val).build());
    }
    return scopes;
  }

  public static List<Measurement> measure(
      List<Scope> scopes,
      List<Named<IsoldeSpec>> problems,
      List<Solver> solvers,
      List<Named<SynthesizerI>> implementations,
      int samples,
      long timeout_s)
      throws InterruptedException, ExecutionException {
    int uniqueRuns =
        scopes.size() * problems.size() * solvers.size() * implementations.size() * samples;
    int count = 0;
    int success = 0;
    int failed = 0;
    int timeouts = 0;
    int crashes = 0;
    Date run = Date.from(Instant.now());
    List<Measurement> rows = new ArrayList<>(uniqueRuns);
    for (var implementation : implementations) {
      for (Solver solver : solvers) {
        for (var problem : problems) {
          boolean timedOut = false;
          for (int scope_idx = 0; !timedOut && scope_idx < scopes.size(); scope_idx++) {
            Scope scope = scopes.get(scope_idx);
            IsoldeInput input = new IsoldeInput(scope, problem, implementation, solver);
            Options options = new Options();
            options.setSolver(solver.getSolver());
            for (int sample = 0; !timedOut && sample < samples; sample++) {

              Instant start = Instant.now();

              // Run each measurement on a dedicated single-thread executor so that, on timeout, we
              // can shutdownNow() + awaitTermination() before starting the next run. This is BEST
              // EFFORT: cancellation is cooperative (the CEGIS/naive loops check
              // Thread.interrupted()
              // between solves), but a kodkod SAT solve is a native call that cannot be interrupted
              // mid-solve. A timeout that lands inside a long solve will not stop until that solve
              // returns; awaitTermination below bounds how long we wait and warns if the abandoned
              // run is still going, in which case later timings on this JVM may be skewed. The
              // fully
              // correct fix is to run each solve in its own OS process and destroyForcibly() it on
              // timeout; that is deferred as future work.
              ExecutorService executor = Executors.newSingleThreadExecutor();
              Future<SynthesizedHistory> future =
                  executor.submit(
                      () -> implementation.value().synthesize(scope, problem.value(), options));

              String config =
                  String.format(
                      "(%s, [%s], %s, %s)",
                      implementation.name(), scope, solver.getId(), problem.name());
              String tag = String.format("[%3d/%d] %s", ++count, uniqueRuns, config);

              try {
                SynthesizedHistory hist = future.get(timeout_s, TimeUnit.SECONDS);
                System.out.printf(
                    "%s : %d ms, %d candidates (%s)\n",
                    tag, hist.time(), hist.candidates(), hist.sat() ? "SAT" : "UNSAT");

                if (hist.sat()) {
                  success++;
                } else {
                  failed++;
                }

                rows.add(
                    Measurement.finished(input, hist.cegisResult(), run, Date.from(Instant.now())));
              } catch (TimeoutException e) {
                timedOut = true;
                timeouts++;
                System.out.printf("%s : TIMED OUT (%d s)\n", tag, timeout_s);

                rows.add(
                    Measurement.timeout(input, timeout_s * 1000, run, Date.from(Instant.now())));
              } catch (ExecutionException e) {
                long time = Duration.between(start, Instant.now()).toMillis();
                timedOut = true;
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                assert cause instanceof OutOfMemoryError;
                crashes++;
                System.out.printf("%s : OOM CRASH\n", tag);
                rows.add(Measurement.crash(input, time, run, Date.from(Instant.now())));
              } finally {
                // Best-effort stop of an abandoned run before the next measurement (see the note
                // above the executor: a native solve in progress may not observe the interrupt).
                executor.shutdownNow();
                if (!executor.awaitTermination(timeout_s, TimeUnit.SECONDS)) {
                  System.err.printf(
                      "WARNING: abandoned synthesis for %s did not stop within %d s;"
                          + " later measurements may be affected%n",
                      config, timeout_s);
                }
              }
            }
          }
        }
      }
    }
    System.out.printf(
        "SAT: %d\nUNSAT: %d\nTIMEOUT: %d\nOOM CRASHES: %d\n", success, failed, timeouts, crashes);
    return rows;
  }

  public static void measureAndAppend(
      List<Scope> scopes,
      List<Named<IsoldeSpec>> problems,
      List<Solver> solvers,
      List<Named<SynthesizerI>> implementations,
      int samples,
      long timeout_s,
      Path file)
      throws IOException, InterruptedException, ExecutionException {
    Util.appendMeasurements(
        measure(scopes, problems, solvers, implementations, samples, timeout_s), file);
  }
}
