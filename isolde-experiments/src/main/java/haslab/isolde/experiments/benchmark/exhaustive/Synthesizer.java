package haslab.isolde.experiments.benchmark.exhaustive;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.AbstractHistory;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
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
      Scope scope, ExecutionFormula<BiswasExecution> pos, ExecutionFormula<BiswasExecution> neg) {
    int candidates = 0;
    Instant start = Instant.now();
    ExecutionGenerator generator = new ExecutionGenerator(scope);
    Iterator<AbstractHistory> historyIterator = generator.allHistories();
    Checker checker = new Checker(this.solver);
    while (historyIterator.hasNext()) {
      AbstractHistory history = historyIterator.next();
      candidates++;
      boolean satisfiesPos = false;
      boolean satisfiesNeg = false;
      Iterator<AbstractExecution> executionIterator = generator.allExecutions(history);
      while (!satisfiesNeg && executionIterator.hasNext()) {
        var execution = executionIterator.next();
        satisfiesNeg = checker.check(execution, neg);
        satisfiesPos = checker.check(execution, pos);
      }
      if (satisfiesPos && !satisfiesNeg) {
        long time = Duration.between(start, Instant.now()).toSeconds();
        return new SynthesisSolution(Optional.of(history), candidates, time);
      }
    }
    return new SynthesisSolution(
        Optional.empty(), candidates, Duration.between(start, Instant.now()).toSeconds());
  }
}
