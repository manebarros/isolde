package haslab.isolde.cli;

import haslab.isolde.IsoldeConstraint;
import haslab.isolde.compare.ComparisonMethods;
import haslab.isolde.core.synth.Scope;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "compare",
    mixinStandardHelpOptions = true,
    description =
        "Compare two isolation-level definitions and report which one allows more histories "
            + "(equivalent, one stronger, or incomparable) up to the given scope.")
class CompareCommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      paramLabel = "DEF_A",
      description =
          "First definition as FRAMEWORK:LEVEL, e.g. cerone:SI. Run 'isolde levels' for the "
              + "available definitions.")
  String defA;

  @Parameters(
      index = "1",
      paramLabel = "DEF_B",
      description = "Second definition as FRAMEWORK:LEVEL, e.g. biswas:Snapshot.")
  String defB;

  @Option(names = "--txn", description = "Number of transactions (default: 3).")
  int txn = 3;

  @Option(names = "--obj", description = "Number of objects (default: 3).")
  int obj = 3;

  @Option(names = "--val", description = "Number of values (default: 3).")
  int val = 3;

  @Option(
      names = "--solver",
      description = "SAT solver to use: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).")
  IsoldeCli.Solver solver = IsoldeCli.Solver.SAT4J;

  @Override
  public Integer call() {
    IsoldeConstraint a;
    IsoldeConstraint b;
    try {
      a = Constraints.parse(defA);
      b = Constraints.parse(defB);
    } catch (IllegalArgumentException e) {
      System.err.println("error: " + e.getMessage());
      return ExitCode.USAGE;
    }

    Scope scope = new Scope.Builder().txn(txn).obj(obj).val(val).build();
    System.out.printf("Comparing %s and %s with scope: %s%n%n", defA, defB, scope);
    System.out.println(ComparisonMethods.compare(scope, defA, a, defB, b, solver.factory));
    return ExitCode.OK;
  }
}
