package haslab.isolde.cli;

import haslab.isolde.cli.Constraints.Framework;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(
    name = "levels",
    mixinStandardHelpOptions = true,
    description =
        "List the isolation-level definitions accepted by --require, --forbid and compare.")
class LevelsCommand implements Callable<Integer> {

  @Override
  public Integer call() {
    for (Framework framework : Framework.values()) {
      System.out.printf("%s:%n", framework.prefix());
      // The levels come back in catalog order, so a header whenever the catalog changes groups them
      // by the class that declares them — which is what says whether a level is axiomatic or a
      // characterization by anomalous patterns.
      Class<?> catalog = null;
      for (var level : Constraints.levels(framework).entrySet()) {
        if (level.getValue().catalog() != catalog) {
          catalog = level.getValue().catalog();
          System.out.printf("  %s%n", catalog.getSimpleName());
        }
        System.out.printf("    %s:%s%n", framework.prefix(), level.getKey());
      }
      System.out.println();
    }
    return 0;
  }
}
