package haslab.isolde.experiments;

import haslab.isolde.core.synth.noSession.SimpleScope;
import haslab.isolde.experiments.benchmark.EquivalentDefinitionsAcrossFrameworksWithoutSessions;
import haslab.isolde.experiments.benchmark.Util;
import java.io.IOException;
import java.util.List;

public class Main {
  public static void main(String[] args) throws IOException {
    List<SimpleScope> scopes = Util.simpleScopesFromRange(5, 5, 4, 5);
    var levels = Util.levels.keySet();
    var solvers = Util.solvers.keySet();
    var file = "/home/mane/Desktop/vldb-experiments/withoutSessions/unsat_diff_frameworks.csv";
    EquivalentDefinitionsAcrossFrameworksWithoutSessions.measureAndWrite(
        scopes, levels, solvers, 3, file);
  }
}
