package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.EquivalentDefinitionsAcrossFrameworks;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    // DifferentDefinitionsCerone.measureAndWrite("/home/mane/Desktop/vldb-experiments/data.csv");
    // DifferentDefinitionsBiswas.measureAndAppend("/home/mane/Desktop/vldb-experiments/data.csv");
    // DifferentDefinitionsAcrossFrameworks.measureAndWrite(
    // "/home/mane/Desktop/vldb-experiments/edges_diff_frameworks.csv");
    EquivalentDefinitionsAcrossFrameworks.measureAndAppend(
        "/home/mane/Desktop/vldb-experiments/equiv_diff_frameworks_temp.csv");
    // VerifyBiswasAndCeroneEquivalence.verify(5);
  }
}
