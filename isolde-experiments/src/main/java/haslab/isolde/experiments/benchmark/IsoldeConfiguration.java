package haslab.isolde.experiments.benchmark;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.IsoldeSynthesizer;
import haslab.isolde.SynthesizedHistory;
import haslab.isolde.core.synth.Scope;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class IsoldeConfiguration {

  public static record NamedProblem(IsoldeSpec problem, String name) {}

  private IsoldeSynthesizer.Builder builder;
  private String name;

  public IsoldeConfiguration(String name, IsoldeSynthesizer.Builder builder) {
    this.builder = builder;
    this.name = name;
  }

  public List<SimpleMeasurement> measure(
      List<Scope> scopes, List<NamedProblem> problems, Collection<String> solvers, int samples) {
    int uniqueRuns = scopes.size() * problems.size() * solvers.size() * samples;
    int count = 0;
    int success = 0;
    int failed = 0;
    Date run = Date.from(Instant.now());
    List<SimpleMeasurement> rows = new ArrayList<>(uniqueRuns);
    for (Scope scope : scopes) {
      for (NamedProblem problem : problems) {
        for (String solver : solvers) {
          IsoldeSynthesizer synthesizer = this.builder.solver(Util.getSolver(solver)).build();
          for (int sample = 0; sample < samples; sample++) {

            SynthesizedHistory hist = synthesizer.synthesize(scope, problem.problem());

            System.out.printf(
                "[%3d/%d] (%s, [%s], %s, %s) : %d ms\n",
                ++count, uniqueRuns, this.name, scope, solver, problem.name(), hist.time());

            if (hist.sat()) {
              success++;
            } else {
              failed++;
            }

            int candidates = hist.candidates();
            rows.add(
                new SimpleMeasurement(
                    this.name,
                    solver,
                    problem.name(),
                    scope,
                    hist.time(),
                    candidates,
                    run,
                    Date.from(Instant.now())));
          }
        }
      }
    }
    System.out.printf("SAT: %d\nUNSAT: %d\n", success, failed);
    return rows;
  }

  public void measureAndWrite(
      List<Scope> scopes,
      List<NamedProblem> problems,
      Collection<String> solvers,
      int samples,
      Path file)
      throws IOException {
    List<SimpleMeasurement> measurements = measure(scopes, problems, solvers, samples);
    Util.writeSimpleMeasurements(measurements, file);
  }

  public void measureAndWrite(
      List<Scope> scopes,
      List<NamedProblem> problems,
      Collection<String> solvers,
      int samples,
      String file)
      throws IOException {
    measureAndWrite(scopes, problems, solvers, samples, Path.of(file));
  }

  public void measureAndAppend(
      List<Scope> scopes,
      List<NamedProblem> problems,
      Collection<String> solvers,
      int samples,
      Path file)
      throws IOException {
    List<SimpleMeasurement> measurements = measure(scopes, problems, solvers, samples);
    Util.appendSimpleMeasurements(measurements, file);
  }

  public void measureAndAppend(
      List<Scope> scopes,
      List<NamedProblem> problems,
      Collection<String> solvers,
      int samples,
      String file)
      throws IOException {
    measureAndAppend(scopes, problems, solvers, samples, Path.of(file));
  }
}
