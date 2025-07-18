package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.DifferentDefinitionsCerone;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    DifferentDefinitionsCerone.measureAndWrite("~/sat_cerone.csv");
  }
}
