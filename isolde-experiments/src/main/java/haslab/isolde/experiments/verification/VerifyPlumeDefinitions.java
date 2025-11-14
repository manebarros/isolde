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
              TransactionalAnomalousPatterns.ReadAtomicV3));

  public static void verify(int scope) {
    verify(new Scope(scope));
  }

  public static void verify(Scope s) {
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
    Synthesizer synth = new Synthesizer(new Scope.Builder(2).txn(3).obj(1).build());
    synth.registerBiswas(
        new SynthesisSpec<>(
            TransactionalAnomalousPatterns.ReadAtomicV1, AxiomaticDefinitions.ReadAtomic.not()));
    return synth.synthesize().history();
  }
}
