package haslab.isolde.core.naive;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistorySchema;
import haslab.isolde.core.cegis.CegisResult;
import haslab.isolde.core.cegis.CegisResult.Counterexample;
import haslab.isolde.core.cegis.CegisResult.FailedCandidate;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.check.candidate.CandidateChecker;
import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.HistoryConstraintProblem;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.kodkod.KodkodProblem;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Instance;

public class NaiveSynthesizer<T, S> {

  private final HistoryConstraintProblem<FolSynthesisInput, T, S> synthesisEncoder;
  private final List<Verifier<?>> verifiers;

  public NaiveSynthesizer(HistoryConstraintProblem<FolSynthesisInput, T, S> synthesisEncoder) {
    this.synthesisEncoder = synthesisEncoder;
    this.verifiers = new ArrayList<>();
  }

  private record Verifier<E extends Execution>(
      CandidateChecker<E> checker, ExecutionFormula<E> universalFormula) {

    public Optional<Counterexample<E>> verify(
        HistorySchema history, Instance instance, Solver solver) {
      Solution candCheckSol = checker.check(instance, history, universalFormula.not(), solver);

      if (candCheckSol.unsat()) {
        // No counterexample
        return Optional.empty();
      }

      return Optional.of(
          new Counterexample<>(candCheckSol.instance(), checker.execution(), universalFormula));
    }
  }

  private List<? extends Counterexample<? extends Execution>> verify(
      HistorySchema history, Instance instance, Solver solver) {

    for (var verifier : this.verifiers) {
      var maybeCounterexample = verifier.verify(history, instance, solver);

      if (maybeCounterexample.isPresent()) {
        return Collections.singletonList(maybeCounterexample.get());
      }
    }
    return new ArrayList<>();
  }

  private static <E extends Execution> List<ExecutionFormula<E>> searchFormulas(
      SynthesisSpec<E> spec) {
    List<ExecutionFormula<E>> formulas = new ArrayList<>(spec.existentialFormulas());
    if (spec.hasUniversal()) {
      formulas.add(spec.universalFormula().get());
    }
    return formulas;
  }

  public <E extends Execution> List<E> register(
      SynthesisSpec<E> spec,
      ExecutionModuleConstructor<E, FolSynthesisInput, S, ?> encoderConstructor,
      CandidateChecker<E> checker) {

    if (spec.hasUniversal()) {
      this.verifiers.add(new Verifier<>(checker, spec.universalFormula().get()));
    }
    return this.synthesisEncoder.register(encoderConstructor, searchFormulas(spec));
  }

  public CegisResult synthesize(Options synthOptions, Options checkOptions) {
    KodkodProblem searchProblem = this.synthesisEncoder.encode();

    Solver synthesizer = new Solver(synthOptions);
    Solver checker = new Solver(checkOptions);

    List<FailedCandidate> failedCandidates = new ArrayList<>();
    List<? extends Counterexample<? extends Execution>> counterexamples;

    Instant start = Instant.now();
    Instant synthStart = Instant.now();

    Iterator<Solution> solutions =
        synthesizer.solveAll(searchProblem.formula(), searchProblem.bounds());

    Solution sol = solutions.next();
    long synthTime = Duration.between(synthStart, Instant.now()).toMillis();
    int firstSynthClauses = sol.stats().clauses();

    long checkTime = 0;
    Instant checkStart = Instant.now();
    while (sol.sat()
        && !(counterexamples = verify(historyEncoding(), sol.instance(), checker)).isEmpty()) {
      checkTime += Duration.between(checkStart, Instant.now()).toMillis();
      failedCandidates.add(
          new FailedCandidate(
              sol.instance(), counterexamples)); // is this right in terms of pointers?
      synthStart = Instant.now();
      sol = solutions.next();
      synthTime += Duration.between(synthStart, Instant.now()).toMillis();

      checkStart = Instant.now();
    }
    checkTime += Duration.between(checkStart, Instant.now()).toMillis();
    long time = Duration.between(start, Instant.now()).toMillis();

    int totalSynthClauses = sol.stats().clauses();
    return sol.sat()
        ? CegisResult.success(
            historyEncoding(),
            sol.instance(),
            failedCandidates,
            firstSynthClauses,
            totalSynthClauses,
            synthTime,
            checkTime,
            time)
        : CegisResult.fail(
            historyEncoding(),
            failedCandidates,
            firstSynthClauses,
            totalSynthClauses,
            synthTime,
            checkTime,
            time);
  }

  public CegisResult synthesize() {
    return synthesize(new Options());
  }

  public CegisResult synthesize(SATFactory satSolver) {
    Options opt = new Options();
    opt.setSolver(satSolver);
    return synthesize(opt);
  }

  public CegisResult synthesize(Options options) {
    return synthesize(options, options);
  }

  public HistorySchema historyEncoding() {
    return this.synthesisEncoder.historyEncoding();
  }
}
