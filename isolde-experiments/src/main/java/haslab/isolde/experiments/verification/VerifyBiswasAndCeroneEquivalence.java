package haslab.isolde.experiments.verification;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.synth.Scope;
import java.util.Arrays;
import java.util.List;

public final class VerifyBiswasAndCeroneEquivalence {

  private VerifyBiswasAndCeroneEquivalence() {}

  private static record Definition(
      String name,
      ExecutionFormula<CeroneExecution> ceroneDef,
      ExecutionFormula<BiswasExecution> biswasDef) {}

  private static final List<Definition> levels =
      Arrays.asList(
          new Definition("RA", CeroneDefinitions.RA, AxiomaticDefinitions.ReadAtomic),
          new Definition("CC", CeroneDefinitions.CC, AxiomaticDefinitions.Causal),
          new Definition("PC", CeroneDefinitions.PC, AxiomaticDefinitions.Prefix),
          new Definition("SI", CeroneDefinitions.SI, AxiomaticDefinitions.Snapshot),
          new Definition("SER", CeroneDefinitions.SER, AxiomaticDefinitions.Ser));

  public static void verify(int scope) {
    for (var def : levels) {
      ComparisonResult result =
          ComparisonMethods.compareBiswasCerone(
              new Scope(scope),
              "Cerone's " + def.name(),
              def.ceroneDef(),
              "Biswas' " + def.name(),
              def.biswasDef());
      System.out.println(result + "\n" + result.timeInfoString());
    }
  }
}
