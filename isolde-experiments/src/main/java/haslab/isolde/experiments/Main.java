package haslab.isolde.experiments;

import haslab.isolde.experiments.verification.FeketeReadOnlyAnomaly;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    FeketeReadOnlyAnomaly.generateAnomalyBiswas();
  }
}
