package haslab.isolde.experiments.verification;

import haslab.isolde.Synthesizer;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.biswas.definitions.TransactionalAnomalousPatterns;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.History;
import java.util.Arrays;
import java.util.List;

public final class VerifyPlumeDefinitions {
  private VerifyPlumeDefinitions() {}

  private static record Level(
      String name,
      ExecutionFormula<BiswasExecution> axiomaticDefinition,
      ExecutionFormula<BiswasExecution> anomalyDefinition) {}

  private static final List<Level> levels =
      Arrays.asList(
          new Level(
              "Read Atomic",
              AxiomaticDefinitions.ReadAtomic,
              TransactionalAnomalousPatterns.ReadAtomic),
          new Level(
              "Causal Consistency",
              AxiomaticDefinitions.Causal,
              TransactionalAnomalousPatterns.Causal));

  public static void verify(int scope) {
    Scope s = new Scope(scope);
    for (var level : levels) {
      var name = level.name();
      ComparisonResult result =
          ComparisonMethods.compareBiswas(
              s,
              "Axiomatic " + name,
              level.axiomaticDefinition(),
              "Anomaly-based " + name,
              level.anomalyDefinition());
      System.out.println(result);
    }
  }

  public static History historyAllowedByPlumeRaButNotByBiswasRa() {
    Synthesizer synth = new Synthesizer(new Scope(2, 1, 2, 1));
    synth.registerBiswas(
        new SynthesisSpec<>(
            TransactionalAnomalousPatterns.ReadAtomic, AxiomaticDefinitions.ReadAtomic.not()));
    return synth.synthesize().history();
  }
}
