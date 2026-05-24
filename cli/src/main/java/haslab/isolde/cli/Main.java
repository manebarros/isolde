package haslab.isolde.cli;

import picocli.CommandLine;

public final class Main {
  private Main() {}

  public static void main(String[] args) {
    int exitCode =
        new CommandLine(new IsoldeCli()).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
    System.exit(exitCode);
  }
}
