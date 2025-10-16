package haslab.isolde.experiments.benchmark;

import haslab.isolde.SynthesizedHistory;
import haslab.isolde.Synthesizer;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public final class EquivalentDefinitionsAcrossFrameworks {
  private EquivalentDefinitionsAcrossFrameworks() {}

  private static record Definition(
      String name,
      ExecutionFormula<CeroneExecution> ceroneDef,
      ExecutionFormula<BiswasExecution> biswasDef) {}

  private static final List<Scope> scopes = Util.scopesFromRange(5, 5, 3, 3, 5);

  private static final List<Definition> levels =
      Arrays.asList(
          new Definition("RA", CeroneDefinitions.RA, AxiomaticDefinitions.ReadAtomic),
          new Definition("CC", CeroneDefinitions.CC, AxiomaticDefinitions.Causal),
          new Definition("PC", CeroneDefinitions.PC, AxiomaticDefinitions.Prefix),
          new Definition("Ser", CeroneDefinitions.SER, AxiomaticDefinitions.Ser),
          new Definition("SI", CeroneDefinitions.SI, AxiomaticDefinitions.Snapshot));

  public static final List<Measurement> measure(
      List<Scope> scopes, List<Definition> levels, Collection<String> solvers, int samples) {
    int uniqueRuns = scopes.size() * levels.size() * solvers.size() * samples * 2;
    int count = 0;
    int success = 0;
    int failed = 0;
    Instant run = Instant.now();
    List<Measurement> rows = new ArrayList<>(uniqueRuns);
    for (Scope scope : scopes) {
      for (Definition level : levels) {
        //  Biswas and not Cerone
        Synthesizer synth = new Synthesizer(scope);
        synth.registerBiswas(new SynthesisSpec<>(level.biswasDef()));
        synth.registerCerone(SynthesisSpec.fromUniversal(level.ceroneDef().not()));

        // Cerone and not Biswas
        Synthesizer synth2 = new Synthesizer(scope);
        synth2.registerCerone(new SynthesisSpec<>(level.ceroneDef()));
        synth2.registerBiswas(SynthesisSpec.fromUniversal(level.biswasDef().not()));

        for (String solver : solvers) {
          for (int sample = 0; sample < samples; sample++) {
            SynthesizedHistory hist = synth.synthesize(Util.solvers.get(solver));
            long time = hist.time();
            int candidates = hist.candidates();

            if (hist.sat()) {
              success++;
            } else {
              failed++;
            }

            System.out.printf(
                "[%3d/%d] (%s, [%s], %s, %s - Biswas and not Cerone) : %d ms, %d candidates\n",
                ++count,
                uniqueRuns,
                "no_fixed_sessions",
                scope,
                solver,
                level.name(),
                time,
                candidates); // TODO : use different implementations

            rows.add(
                new Measurement(
                    "no_fixed_sessions",
                    solver,
                    level.name() + "_Biswas",
                    level.name() + "_Cerone",
                    scope,
                    time,
                    candidates,
                    Date.from(run),
                    Date.from(Instant.now())));

            hist = synth2.synthesize(Util.solvers.get(solver));
            time = hist.time();
            candidates = hist.candidates();

            if (hist.sat()) {
              success++;
            } else {
              failed++;
            }

            System.out.printf(
                "[%3d/%d] (%s, [%s], %s, %s - Cerone and not Biswas) : %d ms, %d candidates\n",
                ++count,
                uniqueRuns,
                "no_fixed_sessions",
                scope,
                solver,
                level.name(),
                time,
                candidates); // TODO : use different implementations

            rows.add(
                new Measurement(
                    "no_fixed_sessions",
                    solver,
                    level.name() + "_Cerone",
                    level.name() + "_Biswas",
                    scope,
                    time,
                    candidates,
                    Date.from(run),
                    Date.from(Instant.now())));
          }
        }
      }
    }
    System.out.printf("SAT: %d\nUNSAT: %d\n", success, failed);
    return rows;
  }

  public static final void measureAndWrite(String file) throws IOException {
    List<Measurement> measurements = measure(scopes, levels, Util.solvers.keySet(), 3);
    Util.writeMeasurements(measurements, file);
  }

  public static final void measureAndAppend(String file) throws IOException {
    List<Measurement> measurements = measure(scopes, levels, Util.solvers.keySet(), 3);
    Util.appendMeasurements(measurements, file);
  }
}
