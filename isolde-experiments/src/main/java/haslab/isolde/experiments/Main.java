package haslab.isolde.experiments;

import haslab.isolde.experiments.verification.Scenarios;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    System.out.println(Scenarios.runScenario(Scenarios.greenRedItems()));
  }
}
