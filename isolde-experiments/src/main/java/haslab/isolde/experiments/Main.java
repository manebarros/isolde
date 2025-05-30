package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.EquivalentDefinitionsAcrossFrameworks;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    EquivalentDefinitionsAcrossFrameworks.measureAndAppend(
        "/home/mane/Desktop/vldb-experiments/equiv_diff_frameworks_ser_minisat.csv");
  }
}
