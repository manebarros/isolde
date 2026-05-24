package haslab.isolde.experiments.verification;

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
            FeketeReadOnlyAnomaly::updateSerExplicit,
            "Implicit UpdateSer Biswas",
            FeketeReadOnlyAnomaly::updateSer);
    System.out.println(result);

    ComparisonResult result2 =
        ComparisonMethods.compareBiswasCerone(
            s,
            "Right UpdateSer Cerone",
            FeketeReadOnlyAnomaly::updateSer,
            "Implicit UpdateSer Biswas",
            FeketeReadOnlyAnomaly::updateSer);
    System.out.println(result2);
  }
}
