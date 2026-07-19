package haslab.isolde.cli;

import haslab.isolde.IsoldeConstraint;
import haslab.isolde.IsoldeSpec;
import haslab.isolde.IsoldeSynthesizer;
import haslab.isolde.SynthesizedHistory;
import haslab.isolde.core.synth.Scope;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import kodkod.ast.Formula;
import kodkod.engine.satlab.SATFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

@Command(
    name = "isolde",
    mixinStandardHelpOptions = true,
    subcommands = {CompareCommand.class, HelpCommand.class},
    description =
        "Synthesize transactional histories that satisfy (or violate) isolation-level specifications.")
public class IsoldeCli implements Callable<Integer> {

  enum Solver {
    SAT4J(SATFactory.DefaultSAT4J),
    MINISAT(SATFactory.MiniSat),
    GLUCOSE(SATFactory.Glucose);

    final SATFactory factory;

    Solver(SATFactory factory) {
      this.factory = factory;
    }
  }

  @Option(names = "--txn", description = "Number of transactions (default: 3).")
  int txn = 3;

  @Option(names = "--obj", description = "Number of objects (default: 3).")
  int obj = 3;

  @Option(names = "--val", description = "Number of values (default: 3).")
  int val = 3;

  @Option(
      names = "--require",
      paramLabel = "FRAMEWORK:LEVEL",
      description =
          "Require the synthesized history to satisfy the named isolation level. "
              + "FRAMEWORK is one of: biswas, cerone. LEVEL is a public static field on "
              + "AxiomaticDefinitions (biswas) or CeroneDefinitions (cerone), e.g. "
              + "biswas:Ser, cerone:RA. May be specified multiple times.")
  List<String> requires = new ArrayList<>();

  @Option(
      names = "--forbid",
      paramLabel = "FRAMEWORK:LEVEL",
      description = "Forbid the synthesized history from satisfying the named isolation level.")
  List<String> forbids = new ArrayList<>();

  @Option(
      names = "--solver",
      description = "SAT solver to use: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).")
  Solver solver = Solver.SAT4J;

  @Override
  public Integer call() {
    if (requires.isEmpty() && forbids.isEmpty()) {
      System.err.println("error: at least one of --require or --forbid must be specified.");
      return 2;
    }

    IsoldeSpec.Builder specBuilder =
        new IsoldeSpec.Builder(IsoldeConstraint.history(h -> Formula.TRUE));
    for (String s : requires) specBuilder.and(Constraints.parse(s));
    for (String s : forbids) specBuilder.andNot(Constraints.parse(s));

    Scope scope = new Scope.Builder().txn(txn).obj(obj).val(val).build();
    IsoldeSpec spec = specBuilder.build();
    IsoldeSynthesizer synthesizer = new IsoldeSynthesizer.Builder().solver(solver.factory).build();

    System.out.printf("Synthesizing with scope: %s%n", scope);
    SynthesizedHistory result = synthesizer.synthesize(scope, spec);

    if (result.sat()) {
      System.out.printf(
          "Found a history in %d ms (%d candidates explored):%n%n",
          result.time(), result.candidates());
      System.out.println(result);
      return 0;
    } else {
      System.out.printf(
          "No history found (%d ms, %d candidates explored).%n",
          result.time(), result.candidates());
      return 1;
    }
  }
}
