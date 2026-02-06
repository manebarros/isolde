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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

  public static List<Scope> scopesFromRange(int keys, int val, int sessions, int from, int to) {
    return scopesFromRange(keys, val, sessions, from, to, 1);
  }

  public static List<Scope> scopesFromRange(
      int keys, int val, int sessions, int from, int to, int step) {
    List<Scope> scopes = new ArrayList<>();
    for (int txn_num = from; txn_num <= to; txn_num += step) {
      scopes.add(new Scope.Builder().txn(txn_num).obj(keys).val(val).sess(sessions).build());
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

              CompletableFuture<SynthesizedHistory> future =
                  CompletableFuture.supplyAsync(
                      () -> implementation.value().synthesize(scope, problem.value(), options));

              try {
                SynthesizedHistory hist = future.get(timeout_s, TimeUnit.SECONDS);
                System.out.printf(
                    "[%3d/%d] (%s, [%s], %s, %s) : %d ms, %d candidates (%s)\n",
                    ++count,
                    uniqueRuns,
                    implementation.name(),
                    scope,
                    solver.getId(),
                    problem.name(),
                    hist.time(),
                    hist.candidates(),
                    hist.sat() ? "SAT" : "UNSAT");

                if (hist.sat()) {
                  success++;
                } else {
                  failed++;
                }

                rows.add(
                    Measurement.finished(input, hist.cegisResult(), run, Date.from(Instant.now())));
              } catch (TimeoutException e) {
                future.cancel(true);
                timedOut = true;
                timeouts++;
                System.out.printf(
                    "[%3d/%d] (%s, [%s], %s, %s) : TIMED OUT (%d s)\n",
                    ++count,
                    uniqueRuns,
                    implementation.name(),
                    scope,
                    solver.getId(),
                    problem.name(),
                    timeout_s);

                rows.add(
                    Measurement.timeout(input, timeout_s * 1000, run, Date.from(Instant.now())));
              } catch (OutOfMemoryError e) {
                future.cancel(true);
                timedOut = true;
                crashes++;
                System.out.printf(
                    "[%3d/%d] (%s, [%s], %s, %s) : OOM CRASH\n",
                    ++count,
                    uniqueRuns,
                    implementation.name(),
                    scope,
                    solver.getId(),
                    problem.name());

                rows.add(
                    Measurement.timeout(input, timeout_s * 1000, run, Date.from(Instant.now())));
              }
            }
          }
        }
      }
    }
    System.out.printf(
        "SAT: %d\nUNSAT: %d\nTIMEOUT: %d\nOOM CRASHES: %d", success, failed, timeouts, crashes);
    return rows;
  }

  public static void measureAndWrite(
      List<Scope> scopes,
      List<Named<IsoldeSpec>> problems,
      List<Solver> solvers,
      List<Named<SynthesizerI>> implementations,
      int samples,
      long timeout_s,
      Path file)
      throws IOException, InterruptedException, ExecutionException {
    Util.writeMeasurements(
        measure(scopes, problems, solvers, implementations, samples, timeout_s), file);
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
