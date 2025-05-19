package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.CompareDifferentFrameworksBenchmark;
import java.io.IOException;
import java.nio.file.Path;

public class Main {
  public static void main(String[] args) throws IOException {
    CompareDifferentFrameworksBenchmark.measureBiswasNotCerone(
        Path.of("/home/mane/Desktop/vldb-experiments/data.csv"));
  }
}
