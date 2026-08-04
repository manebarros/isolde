package haslab.isolde.experiments.verification;

import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.compare.ComparisonMethods;
import haslab.isolde.compare.ComparisonResult;
import haslab.isolde.core.synth.Scope;

public final class VerifyUpdateSerDefinitions {
  private VerifyUpdateSerDefinitions() {}

  public static void verify(int scope) {
    verify(new Scope(scope));
  }

  public static void verify(Scope s) {
    ComparisonResult result =
        ComparisonMethods.compareBiswas(
            s,
            "Explicit UpdateSer Biswas",
            AxiomaticDefinitions.UpdateSerExplicit,
            "Implicit UpdateSer Biswas",
            AxiomaticDefinitions.UpdateSer);
    System.out.println(result);

    ComparisonResult result2 =
        ComparisonMethods.compareBiswasCerone(
            s,
            "Right UpdateSer Cerone",
            CeroneDefinitions.UpdateSer,
            "Implicit UpdateSer Biswas",
            AxiomaticDefinitions.UpdateSer);
    System.out.println(result2);
  }
}
