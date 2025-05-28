package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.DifferentDefinitionsBiswas;
import haslab.isolde.experiments.benchmark.DifferentDefinitionsCerone;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    DifferentDefinitionsCerone.measureAndWrite("/home/mane/Desktop/vldb-experiments/data.csv");
    DifferentDefinitionsBiswas.measureAndAppend("/home/mane/Desktop/vldb-experiments/data.csv");
    // DifferentDefinitionsAcrossFrameworks.measure("/home/mane/Desktop/vldb-experiments/data.csv");
    // EquivalentDefinitionsAcrossFrameworks.measure("/home/mane/Desktop/vldb-experiments/data.csv");
    // VerifyBiswasAndCeroneEquivalence.verify(5);
  }
}
