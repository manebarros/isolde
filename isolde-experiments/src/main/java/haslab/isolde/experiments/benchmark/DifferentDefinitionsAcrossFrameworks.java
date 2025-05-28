package haslab.isolde.experiments.benchmark;

import haslab.isolde.Synthesizer;
import haslab.isolde.Synthesizer.CegisHistory;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
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

public final class DifferentDefinitionsAcrossFrameworks {
  private DifferentDefinitionsAcrossFrameworks() {}

  private static record Definition(
      String name,
      ExecutionFormula<CeroneExecution> ceroneDef,
      ExecutionFormula<BiswasExecution> biswasDef) {}

  private static record Edge(String weaker, String stronger) {}

  private static final Map<String, Definition> levels =
      Map.of(
          "RA", new Definition("RA", CeroneDefinitions.RA, AxiomaticDefinitions.ReadAtomic),
          "CC", new Definition("CC", CeroneDefinitions.CC, AxiomaticDefinitions.Causal),
          "PC", new Definition("PC", CeroneDefinitions.PC, AxiomaticDefinitions.Prefix),
          "SI", new Definition("SI", CeroneDefinitions.SI, AxiomaticDefinitions.Snapshot),
          "Ser", new Definition("Ser", CeroneDefinitions.SER, AxiomaticDefinitions.Ser));

  private static final List<Edge> edges =
      Arrays.asList(
          new Edge("RA", "CC"), new Edge("CC", "PC"), new Edge("PC", "SI"), new Edge("SI", "Ser"));

  private static final List<Scope> scopes = Util.scopesFromRange(3, 3, 3, 4, 8);

  public static final List<Measurement> measure(
      List<Scope> scopes, Collection<Edge> edges, Collection<String> solvers, int samples) {
    int uniqueRuns = scopes.size() * edges.size() * solvers.size() * samples * 2;
    int count = 0;
    int success = 0;
    int failed = 0;
    Date run = Date.from(Instant.now());
    List<Measurement> rows = new ArrayList<>(uniqueRuns);
    for (Scope scope : scopes) {
      for (Edge edge : edges) {
        //  Biswas and not Cerone
        Synthesizer synth = new Synthesizer(scope);
        synth.registerBiswas(new SynthesisSpec<>(levels.get(edge.weaker()).biswasDef()));
        synth.registerCerone(
            SynthesisSpec.fromUniversal(levels.get(edge.stronger()).ceroneDef().not()));

        // Cerone and not Biswas
        Synthesizer synth2 = new Synthesizer(scope);
        synth2.registerCerone(new SynthesisSpec<>(levels.get(edge.weaker()).ceroneDef()));
        synth2.registerBiswas(
            SynthesisSpec.fromUniversal(levels.get(edge.stronger()).biswasDef().not()));

        for (String solver : solvers) {
          for (int sample = 0; sample < samples; sample++) {
            Instant before = Instant.now();
            CegisHistory hist = synth.synthesizeWithInfo(Util.solvers.get(solver));
            Instant after = Instant.now();
            long time = Duration.between(before, after).toMillis();

            System.out.printf(
                "[%3d/%d] (%s, [%s], Biswas' %s and not Cerone's %s) : %d\n",
                ++count,
                uniqueRuns,
                "default",
                scope,
                edge.weaker(),
                edge.stronger(),
                time); // TODO : use different implementations

            if (hist.history().isPresent()) {
              success++;
            } else {
              failed++;
              System.out.println(
                  String.format(
                      "Biswas' %s and not Cerone's %s\n", edge.weaker(), edge.stronger()));
            }

            int candidates = hist.candidates();
            rows.add(
                new Measurement(
                    "default",
                    solver,
                    edge.weaker() + "_Biswas",
                    edge.stronger() + "_Cerone",
                    scope,
                    time,
                    candidates,
                    run,
                    Date.from(Instant.now())));

            before = Instant.now();
            hist = synth2.synthesizeWithInfo(Util.solvers.get(solver));
            after = Instant.now();
            time = Duration.between(before, after).toMillis();

            System.out.printf(
                "[%3d/%d] (%s, [%s], Cerone's %s and not Biswas' %s) : %d\n",
                ++count,
                uniqueRuns,
                "default",
                scope,
                edge.weaker(),
                edge.stronger(),
                time); // TODO : use different implementations

            if (hist.history().isPresent()) {
              success++;
            } else {
              failed++;
              System.out.println(
                  String.format(
                      "Cerone's' %s and not Biswas' %s\n", edge.weaker(), edge.stronger()));
            }

            candidates = hist.candidates();
            rows.add(
                new Measurement(
                    "default",
                    solver,
                    edge.weaker() + "_Cerone",
                    edge.stronger() + "_Biswas",
                    scope,
                    time,
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

  public static final void measure(String file) throws IOException {
    measure(Path.of(file));
  }

  public static final void measure(Path file) throws IOException {
    List<Measurement> measurements = measure(scopes, edges, Util.solvers.keySet(), 3);
    Util.writeMeasurements(measurements, file);
  }
}
