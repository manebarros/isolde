package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.DifferentDefinitionsAcrossFrameworks;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    // List<SimpleScope> scopes = Util.simpleScopesFromRange(5, 5, 4, 5);
    // var levels = Util.levels.keySet();
    // var solvers = Util.solvers.keySet();
    // var file = "/home/mane/Desktop/vldb-experiments/withoutSessions/unsat_diff_frameworks.csv";
    // EquivalentDefinitionsAcrossFrameworksWithoutSessions.measureAndWrite(
    //    scopes, levels, solvers, 3, file);
    DifferentDefinitionsAcrossFrameworks.measureAndWrite(
        "/home/mane/Desktop/vldb-experiments/updated/sat_across_frameworks.csv");
  }
}
