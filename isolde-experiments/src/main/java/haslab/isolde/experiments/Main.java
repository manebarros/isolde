package haslab.isolde.experiments;

import haslab.isolde.core.synth.Scope;
import haslab.isolde.experiments.benchmark.exhaustive.AbstractExecution;
import haslab.isolde.experiments.benchmark.exhaustive.ExecutionGenerator;
import java.util.Iterator;

public class Main {
  public static void main(String[] args) throws Exception {
    // CommandLine cmd = new CommandLine(new Cli()).setCaseInsensitiveEnumValuesAllowed(true);
    // int exitCode = cmd.execute(args);
    // System.exit(exitCode);
    Scope scope = new Scope(3);
    ExecutionGenerator generator = new ExecutionGenerator(scope);
    Iterator<AbstractExecution> executionIterator = generator.allExecutions();
    int counter = 0;
    while (executionIterator.hasNext()) {
      var execution = executionIterator.next();
      System.out.println(execution);
      System.out.println(++counter);
    }
  }
}
