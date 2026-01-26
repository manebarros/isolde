package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.Cli;
import picocli.CommandLine;

public class Main {
  public static void main(String[] args) throws Exception {
    CommandLine cmd = new CommandLine(new Cli()).setOptionsCaseInsensitive(true);
    int exitCode = cmd.execute(args);
    System.exit(exitCode);
  }
}
