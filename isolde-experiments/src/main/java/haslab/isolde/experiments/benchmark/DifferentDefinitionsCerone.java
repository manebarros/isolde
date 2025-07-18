package haslab.isolde.experiments.benchmark;

import haslab.isolde.Synthesizer;
import haslab.isolde.Synthesizer.CegisHistory;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.core.synth.noSession.SimpleScope;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class DifferentDefinitionsCerone {
  private DifferentDefinitionsCerone() {}

  private static record Edge(
      String weakerName,
      ExecutionFormula<CeroneExecution> weakerDef,
      String strongerName,
      ExecutionFormula<CeroneExecution> strongerDef) {}

  private static final List<Scope> scopes = Util.scopesFromRange(5, 5, 3, 4, 10);

  private static final List<Edge> edges =
      Arrays.asList(
          new Edge("RA", CeroneDefinitions.RA, "UA", CeroneDefinitions.UA),
          new Edge("RA", CeroneDefinitions.RA, "CC", CeroneDefinitions.CC),
          new Edge("UA", CeroneDefinitions.UA, "PSI", CeroneDefinitions.PSI),
          new Edge("CC", CeroneDefinitions.CC, "PSI", CeroneDefinitions.PSI),
          new Edge("CC", CeroneDefinitions.CC, "PC", CeroneDefinitions.PC),
          new Edge("PSI", CeroneDefinitions.PSI, "SI", CeroneDefinitions.SI),
          new Edge("PC", CeroneDefinitions.PC, "SI", CeroneDefinitions.SI),
          new Edge("SI", CeroneDefinitions.SI, "Ser", CeroneDefinitions.SER));

  public static final List<Measurement> measure(
      List<Scope> scopes, List<Edge> levels, Collection<String> solvers, int samples) {
    int uniqueRuns = scopes.size() * levels.size() * solvers.size() * samples * 3;
    int count = 0;
    int success = 0;
    int failed = 0;
    Date run = Date.from(Instant.now());
    List<Measurement> rows = new ArrayList<>(uniqueRuns);
    for (Scope scope : scopes) {
      for (Edge edge : levels) {

        Synthesizer synthOptimized = new Synthesizer(new SimpleScope(scope));
        synthOptimized.registerCerone(
            new SynthesisSpec<>(edge.weakerDef(), edge.strongerDef().not()));

        Synthesizer synthNoTotalOrder = Synthesizer.withNoTotalOrder(new SimpleScope(scope));
        synthNoTotalOrder.registerCerone(
            new SynthesisSpec<>(edge.weakerDef(), edge.strongerDef().not()));

        Synthesizer synthNoFixedSessions = new Synthesizer(scope);
        synthNoFixedSessions.registerCerone(
            new SynthesisSpec<>(edge.weakerDef(), edge.strongerDef().not()));

        Map<String, Synthesizer> implementations =
            Map.of("no_fixed_sessions", synthNoFixedSessions);

        for (String implementation : implementations.keySet()) {
          Synthesizer synth = implementations.get(implementation);
          for (String solver : solvers) {
            for (int sample = 0; sample < samples; sample++) {
              Instant before = Instant.now();
              CegisHistory hist = synth.synthesizeWithInfo(Util.solvers.get(solver));
              Instant after = Instant.now();
              long time = Duration.between(before, after).toMillis();

              System.out.printf(
                  "[%3d/%d] (%s, [%s], %s, %s and not %s) : %d ms\n",
                  ++count,
                  uniqueRuns,
                  implementation,
                  scope,
                  solver,
                  edge.weakerName(),
                  edge.strongerName(),
                  time);

              if (hist.history().isPresent()) {
                success++;
              } else {
                failed++;
              }

              int candidates = hist.candidates();
              rows.add(
                  new Measurement(
                      implementation,
                      solver,
                      edge.weakerName() + "_Cerone",
                      edge.strongerName() + "_Cerone",
                      scope,
                      time,
                      candidates,
                      run,
                      Date.from(Instant.now())));
            }
          }
        }
      }
    }
    System.out.printf("SAT: %d\nUNSAT: %d\n", success, failed);
    return rows;
  }

  public static final void measureAndWrite(String file) throws IOException {
    measureAndWrite(Path.of(file));
  }

  public static final void measureAndWrite(Path file) throws IOException {
    List<Measurement> measurements = measure(scopes, edges, Util.solvers.keySet(), 3);
    Util.writeMeasurements(measurements, file);
  }
}
