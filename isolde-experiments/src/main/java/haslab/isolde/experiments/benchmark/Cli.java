package haslab.isolde.experiments.benchmark;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.SynthesizerI;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.experiments.benchmark.Problems.SpecClass;
import haslab.isolde.util.Pair;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "isoldebench",
    mixinStandardHelpOptions = true,
    description = "Record isolde's solving times.")
public class Cli implements Runnable {

  @Parameters(index = "0", description = "Input file name")
  Path destFile;

  @Option(
      names = "--txn",
      description = "Number of transactions (num | start:end)",
      converter = TxnNumConverter.class)
  Pair<Integer> txn_num = new Pair<>(3, 3);

  @Option(names = "--obj", description = "Number of objects")
  Integer obj_num = 3;

  @Option(names = "--val", description = "Number of values")
  Integer val_num = 3;

  @Option(names = "--sess", description = "Number of sessions")
  Integer sess_num = 3;

  @Option(names = "--timeout", description = "Timeout in ms")
  Integer timeout = 300;

  @Option(
      names = {"-s", "--single"},
      description = "Test on a single problem per class")
  boolean single;

  @Option(
      names = "--classes",
      split = ",",
      description = "Comma-separated list of modes: ${COMPLETION-CANDIDATES}")
  List<SpecClass> specClasses = Arrays.asList(SpecClass.values());

  @Option(
      names = "--solvers",
      split = ",",
      description = "Comma-separated list of modes: ${COMPLETION-CANDIDATES}")
  List<Solver> solvers = Arrays.asList(Solver.values());

  @Option(
      names = "--impl",
      split = ",",
      converter = ImplementationConverter.class,
      description = "Execution modes")
  List<Implementation> implementations = Arrays.asList(Implementation.values());

  @Override
  public void run() {
    List<Scope> scopes =
        Util.scopesFromRange(obj_num, val_num, sess_num, txn_num.fst(), txn_num.snd());

    List<List<Named<IsoldeSpec>>> problems = new ArrayList<>();
    for (var specClass : specClasses) {
      if (this.single) {
        problems.add(Collections.singletonList(Problems.getRepresentativeProblem(specClass)));
      } else {
        problems.add(Problems.getProblemSet(specClass));
      }
    }

    List<Named<SynthesizerI>> implementations =
        this.implementations.stream().map(s -> new Named<>(s.getId(), s.getSynthesizer())).toList();

    for (var problemList : problems) {
      try {
        Util.measureAndAppend(
            scopes, problemList, solvers, implementations, 3, this.timeout, destFile);
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }
  }

  static class TxnNumConverter implements ITypeConverter<Pair<Integer>> {

    @Override
    public Pair<Integer> convert(String value) throws Exception {
      if (value.contains(":")) {
        String[] parts = value.split(":");
        if (parts.length != 2) {
          throw new IllegalArgumentException("Range must be start:end");
        }
        return new Pair<>(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      } else {
        int val = Integer.parseInt(value);
        return new Pair<>(val, val + 1);
      }
    }
  }

  static class ImplementationConverter implements ITypeConverter<Implementation> {

    @Override
    public Implementation convert(String value) throws Exception {
      return switch (value.toLowerCase()) {
        case "all" -> Implementation.CEGIS_ALL;
        case "no_smart_search" -> Implementation.CEGIS_NO_SMART_SEARCH;
        case "no_fixed_co" -> Implementation.CEGIS_NO_FIXED_COMMIT_ORDER;
        case "no_incremental" -> Implementation.CEGIS_NO_INC_SOLVING;
        case "no_learning" -> Implementation.NO_LEARNING;
        default -> throw new TypeConversionException("Invalid mode: " + value);
      };
    }
  }
}
