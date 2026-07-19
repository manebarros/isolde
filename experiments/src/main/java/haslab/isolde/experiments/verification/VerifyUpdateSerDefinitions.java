package haslab.isolde.experiments.verification;

import haslab.isolde.core.synth.Scope;
import haslab.isolde.experiments.benchmark.UpdateSerDefinitions;

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
            FeketeReadOnlyAnomaly::updateSerExplicit,
            "Implicit UpdateSer Biswas",
            UpdateSerDefinitions::updateSer);
    System.out.println(result);

    ComparisonResult result2 =
        ComparisonMethods.compareBiswasCerone(
            s,
            "Right UpdateSer Cerone",
            UpdateSerDefinitions::updateSer,
            "Implicit UpdateSer Biswas",
            UpdateSerDefinitions::updateSer);
    System.out.println(result2);
  }
}
