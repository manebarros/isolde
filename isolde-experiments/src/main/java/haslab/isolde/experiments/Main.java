package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.DifferentDefinitionsAcrossFrameworks;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    DifferentDefinitionsAcrossFrameworks.measure("/home/mane/Desktop/vldb-experiments/data.csv");
  }
}
