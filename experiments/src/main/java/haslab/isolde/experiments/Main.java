package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.Cli;
import picocli.CommandLine;

public class Main {
  public static void main(String[] args) {
    CommandLine cmd = new CommandLine(new Cli()).setCaseInsensitiveEnumValuesAllowed(true);
    int exitCode = cmd.execute(args);
    System.exit(exitCode);
  }
}
