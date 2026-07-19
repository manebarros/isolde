package haslab.isolde.experiments.benchmark;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.experiments.benchmark.Problems.SpecClass;
import haslab.isolde.experiments.benchmark.exhaustive.ExhaustiveRunner;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "isoldebench",
    mixinStandardHelpOptions = true,
    description = "Record isolde's solving times.")
public class Cli implements Callable<Integer> {

  @Parameters(index = "0", description = "Input file name")
  Path destFile;

  @Option(
      names = "--txn",
      description = "Number of transactions (num | start:end)",
      converter = TxnNumConverter.class)
  Range txn_num = new Range(3, 3);

  @Option(
      names = "--obj",
      description = "Number of objects (num | start:end)",
      converter = TxnNumConverter.class)
  Range obj_num = new Range(3, 3);

  @Option(
      names = "--val",
      description = "Number of values (num | start:end)",
      converter = TxnNumConverter.class)
  Range val_num = new Range(3, 3);

  @Option(names = "--timeout", description = "Timeout in seconds")
  Integer timeout_s = 300;

  @Option(
      names = {"-s", "--single"},
      description = "Test on a single problem per class")
  boolean single;

  @ArgGroup(exclusive = true, multiplicity = "0..1")
  ProblemsToSolve problems = new ProblemsToSolve();

  static class ProblemsToSolve {
    @Option(
        names = "--classes",
        split = ",",
        description = "Comma-separated list of problem classes: ${COMPLETION-CANDIDATES}")
    List<SpecClass> specClasses = Arrays.asList(SpecClass.values());

    @Option(
        names = {"-p", "--problems"},
        description = "Problems")
    String problem_id;
  }

  @Option(
      names = "--solvers",
      split = ",",
      description = "Comma-separated list of modes: ${COMPLETION-CANDIDATES}")
  List<Solver> solvers = Arrays.asList(Solver.values());

  @Option(
      names = "--impl",
      split = ",",
      converter = ImplementationConverter.class,
      description =
          "Comma-separated implementations: all, none, no_smart_search, no_fixed_co,"
              + " no_incremental, no_learning, exhaustive")
  List<SynthesisRunner> implementations =
      new ArrayList<SynthesisRunner>(Arrays.asList(Implementation.values()));

  @Override
  public Integer call() {
    List<Scope> scopes = Util.scopesFromRange(txn_num, obj_num, val_num);

    List<Named<IsoldeSpec>> problems = new ArrayList<>();
    if (this.problems.problem_id != null) {
      System.out.println("not implemented yet");
      return ExitCode.USAGE;
    }
    for (var specClass : this.problems.specClasses) {
      if (this.single) {
        problems.add(Problems.getRepresentativeProblem(specClass));
      } else {
        for (var problem : Problems.getProblemSet(specClass)) {
          problems.add(problem);
        }
      }
    }

    List<Named<SynthesisRunner>> implementations =
        this.implementations.stream().map(s -> new Named<>(s.id(), s)).toList();

    try {
      Util.measureAndAppend(scopes, problems, solvers, implementations, 3, this.timeout_s, destFile);
    } catch (Exception e) {
      e.printStackTrace();
      return ExitCode.SOFTWARE;
    }
    return ExitCode.OK;
  }

  static class TxnNumConverter implements ITypeConverter<Range> {

    @Override
    public Range convert(String value) throws Exception {
      if (value.contains(":")) {
        String[] parts = value.split(":");
        if (parts.length != 2) {
          throw new IllegalArgumentException("Range must be start:end");
        }
        return new Range(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      } else {
        int val = Integer.parseInt(value);
        return new Range(val, val);
      }
    }
  }

  static class ImplementationConverter implements ITypeConverter<SynthesisRunner> {

    @Override
    public SynthesisRunner convert(String value) throws Exception {
      return switch (value.toLowerCase()) {
        case "all" -> Implementation.CEGIS_ALL;
        case "none" -> Implementation.CEGIS_NONE;
        case "no_smart_search" -> Implementation.CEGIS_NO_SMART_SEARCH;
        case "no_fixed_co" -> Implementation.CEGIS_NO_FIXED_COMMIT_ORDER;
        case "no_incremental" -> Implementation.CEGIS_NO_INC_SOLVING;
        case "no_learning" -> Implementation.NO_LEARNING;
        case "exhaustive" -> new ExhaustiveRunner();
        default -> throw new TypeConversionException("Invalid implementation: " + value);
      };
    }
  }
}
