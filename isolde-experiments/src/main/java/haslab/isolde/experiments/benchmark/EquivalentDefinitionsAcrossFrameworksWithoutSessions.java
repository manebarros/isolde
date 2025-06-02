package haslab.isolde.experiments.benchmark;

import haslab.isolde.Synthesizer;
import haslab.isolde.Synthesizer.CegisHistory;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.noSession.SimpleScope;
import haslab.isolde.experiments.benchmark.Util.LevelDefinitions;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public final class EquivalentDefinitionsAcrossFrameworksWithoutSessions {
  private EquivalentDefinitionsAcrossFrameworksWithoutSessions() {}

  public static final List<Measurement> measure(
      List<SimpleScope> scopes,
      Collection<String> levels,
      Collection<String> solvers,
      int samples) {
    int uniqueRuns = scopes.size() * levels.size() * solvers.size() * samples * 2;
    int count = 0;
    int success = 0;
    int failed = 0;
    Instant run = Instant.now();
    List<Measurement> rows = new ArrayList<>(uniqueRuns);
    for (SimpleScope scope : scopes) {
      for (String levelName : levels) {
        LevelDefinitions level = Util.levels.get(levelName);

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
            Instant before = Instant.now();
            CegisHistory hist = synth.synthesizeWithInfo(Util.solvers.get(solver));
            Instant after = Instant.now();
            long time = Duration.between(before, after).toMillis();
            int candidates = hist.candidates();

            if (hist.history().isPresent()) {
              success++;
            } else {
              failed++;
            }

            System.out.printf(
                "[%3d/%d] (%s, [%s], %s, %s - Biswas and not Cerone) : %d ms, %d candidates\n",
                ++count,
                uniqueRuns,
                "noSession",
                scope,
                solver,
                level.name(),
                time,
                candidates); // TODO : use different implementations

            rows.add(
                new Measurement(
                    "noSession",
                    solver,
                    level.name() + "_Biswas",
                    level.name() + "_Cerone",
                    scope,
                    time,
                    candidates,
                    Date.from(run),
                    Date.from(Instant.now())));

            before = Instant.now();
            hist = synth2.synthesizeWithInfo(Util.solvers.get(solver));
            after = Instant.now();
            time = Duration.between(before, after).toMillis();
            candidates = hist.candidates();

            if (hist.history().isPresent()) {
              success++;
            } else {
              failed++;
            }

            System.out.printf(
                "[%3d/%d] (%s, [%s], %s, %s - Cerone and not Biswas) : %d ms, %d candidates\n",
                ++count,
                uniqueRuns,
                "noSession",
                scope,
                solver,
                level.name(),
                time,
                candidates); // TODO : use different implementations

            rows.add(
                new Measurement(
                    "noSession",
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

  public static final void measureAndWrite(
      List<SimpleScope> scopes,
      Collection<String> levels,
      Collection<String> solvers,
      int samples,
      String file)
      throws IOException {
    List<Measurement> measurements = measure(scopes, levels, solvers, samples);
    Util.writeMeasurements(measurements, file);
  }

  public static final void measureAndAppend(
      List<SimpleScope> scopes,
      Collection<String> levels,
      Collection<String> solvers,
      int samples,
      String file)
      throws IOException {
    List<Measurement> measurements = measure(scopes, levels, solvers, samples);
    Util.appendMeasurements(measurements, file);
  }
}
