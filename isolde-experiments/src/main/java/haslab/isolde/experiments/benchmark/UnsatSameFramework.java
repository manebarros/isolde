package haslab.isolde.experiments.benchmark;

import haslab.isolde.SynthesizedHistory;
import haslab.isolde.Synthesizer;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.biswas.definitions.TransactionalAnomalousPatterns;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public final class UnsatSameFramework {
  private UnsatSameFramework() {}

  private static record Level(
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {}

  private static final List<Scope> scopes = Util.scopesFromRange(5, 5, 3, 4, 10);

  private static final List<Level> levels =
      Arrays.asList(
          new Level(
              "CC_Plume",
              TransactionalAnomalousPatterns.Causal,
              "CC_Biswas",
              AxiomaticDefinitions.Causal),
          new Level(
              "CC_Biswas",
              AxiomaticDefinitions.Causal,
              "CC_Plume",
              TransactionalAnomalousPatterns.Causal));

  public static final List<Measurement> measure(
      List<Scope> scopes, List<Level> levels, Collection<String> solvers, int samples) {
    int uniqueRuns = scopes.size() * levels.size() * solvers.size() * samples;
    int count = 0;
    int success = 0;
    int failed = 0;
    Date run = Date.from(Instant.now());
    List<Measurement> rows = new ArrayList<>(uniqueRuns);
    for (Scope scope : scopes) {
      for (Level edge : levels) {
        Synthesizer synth = new Synthesizer(scope);
        synth.registerBiswas(new SynthesisSpec<>(edge.a_def(), edge.b_def().not()));

        for (String solver : solvers) {
          for (int sample = 0; sample < samples; sample++) {
            SynthesizedHistory hist = synth.synthesize(Util.solvers.get(solver));
            int candidates = hist.candidates();

            if (hist.sat()) {
              success++;
            } else {
              failed++;
            }

            System.out.printf(
                "[%3d/%d] (%s, [%s], %s, %s and not %s) : %d ms, %d candidates\n",
                ++count,
                uniqueRuns,
                "default",
                scope,
                solver,
                edge.a_name(),
                edge.b_name(),
                hist.time(),
                candidates); // TODO : use different implementations

            rows.add(
                new Measurement(
                    "default",
                    solver,
                    edge.a_name() + "_Biswas",
                    edge.b_name() + "_Biswas",
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

  public static final void measureAndWrite(Path file) throws IOException {
    List<Measurement> measurements = measure(scopes, levels, Util.solvers.keySet(), 3);
    Util.writeMeasurements(measurements, file);
  }

  public static final void measureAndAppend(Path file) throws IOException {
    List<Measurement> measurements = measure(scopes, levels, Util.solvers.keySet(), 3);
    Util.appendMeasurements(measurements, file);
  }

  public static final void measureAndWrite(String file) throws IOException {
    measureAndWrite(Path.of(file));
  }

  public static final void measureAndAppend(String file) throws IOException {
    measureAndAppend(Path.of(file));
  }
}
