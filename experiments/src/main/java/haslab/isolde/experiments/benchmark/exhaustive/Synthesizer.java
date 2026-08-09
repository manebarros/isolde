package haslab.isolde.experiments.benchmark.exhaustive;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.AbstractHistory;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;

public class Synthesizer {
  private final Solver solver;

  public Synthesizer(SATFactory solver) {
    Options options = new Options();
    options.setSolver(solver);
    this.solver = new Solver(options);
  }

  public static record SynthesisSolution(
      Optional<AbstractHistory> history, int candidates, long time_ms) {}

  public SynthesisSolution synthesize(
      Scope scope,
      List<ExecutionFormula<BiswasExecution>> pos,
      ExecutionFormula<BiswasExecution> neg) {
    int candidates = 0;
    Instant start = Instant.now();
    ExecutionGenerator generator = new ExecutionGenerator(scope);
    Iterator<AbstractHistory> historyIterator = generator.allHistories();
    Checker checker = new Checker(this.solver);
    while (historyIterator.hasNext()) {
      AbstractHistory history = historyIterator.next();
      candidates++;
      // A history is a witness iff it is allowed by EVERY positive definition (each satisfied by
      // some execution, i.e. its own commit order) and NOT allowed by the negative one. "Allowed by
      // f" means some execution of this history satisfies f, so each positive is OR-accumulated
      // across executions rather than reflecting only the last execution examined.
      boolean[] satisfiesPos = new boolean[pos.size()];
      boolean satisfiesNeg = false;
      Iterator<AbstractExecution> executionIterator = generator.allExecutions(history);
      while (!satisfiesNeg && executionIterator.hasNext()) {
        var execution = executionIterator.next();
        satisfiesNeg = checker.check(execution, neg);
        for (int i = 0; i < pos.size(); i++) {
          if (!satisfiesPos[i]) {
            satisfiesPos[i] = checker.check(execution, pos.get(i));
          }
        }
      }
      if (!satisfiesNeg && allTrue(satisfiesPos)) {
        long time = Duration.between(start, Instant.now()).toMillis();
        return new SynthesisSolution(Optional.of(history), candidates, time);
      }
    }
    return new SynthesisSolution(
        Optional.empty(), candidates, Duration.between(start, Instant.now()).toMillis());
  }

  private static boolean allTrue(boolean[] flags) {
    for (boolean flag : flags) {
      if (!flag) return false;
    }
    return true;
  }
}
