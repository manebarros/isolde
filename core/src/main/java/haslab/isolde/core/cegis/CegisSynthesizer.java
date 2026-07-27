package haslab.isolde.core.cegis;

import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.HistorySchema;
import haslab.isolde.core.cegis.CegisResult.Counterexample;
import haslab.isolde.core.cegis.CegisResult.FailedCandidate;
import haslab.isolde.core.check.candidate.CandidateChecker;
import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.HistoryConstraintProblem;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.kodkod.KodkodProblem;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import kodkod.ast.Formula;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public class CegisSynthesizer<T, S> {

  @FunctionalInterface
  private static interface CandidateSearchFormulaConstructor {
    <E extends Execution> List<ExecutionFormula<E>> construct(SynthesisSpec<E> spec);
  }

  private final HistoryConstraintProblem<FolSynthesisInput, T, S> synthesisEncoder;
  private final List<CegisVerifier<?>> verifiers;
  private final CandidateSearchFormulaConstructor candidateSearchFormulaConstructor;
  private final boolean useIncrementalSolving;

  private CegisSynthesizer(
      HistoryConstraintProblem<FolSynthesisInput, T, S> synthesisEncoder,
      CandidateSearchFormulaConstructor searchFormulaConstructor,
      boolean useIncrementalSolving) {
    this.synthesisEncoder = synthesisEncoder;
    this.verifiers = new ArrayList<>();
    this.candidateSearchFormulaConstructor = searchFormulaConstructor;
    this.useIncrementalSolving = useIncrementalSolving;
  }

  public static <T, S> CegisSynthesizer<T, S> withSmartSearchFormula(
      HistoryConstraintProblem<FolSynthesisInput, T, S> synthesisEncoder,
      boolean incrementalSolving) {
    return new CegisSynthesizer<>(
        synthesisEncoder, CegisSynthesizer::smartSearchFormula, incrementalSolving);
  }

  public static <T, S> CegisSynthesizer<T, S> withNaiveSearchFormula(
      HistoryConstraintProblem<FolSynthesisInput, T, S> synthesisEncoder,
      boolean useIncrementalSolving) {
    return new CegisSynthesizer<>(
        synthesisEncoder, CegisSynthesizer::naiveSearchFormula, useIncrementalSolving);
  }

  private record CegisFeedback<E extends Execution>(
      Counterexample<E> counterexample, HistoryFormula guidingFormula) {}

  private record CegisAggregatedFeedback(
      List<Counterexample<?>> counterexamples, HistoryFormula guidingFormula) {}

  private record CegisVerifier<E extends Execution>(
      CandidateChecker<E> checker,
      CounterexampleEncoder<E> cexEncoder,
      ExecutionFormula<E> universalFormula) {

    Optional<CegisFeedback<E>> verify(
        HistorySchema history, Instance instance, Bounds bounds, Solver solver) {

      Solution candCheckSol = checker.check(instance, history, universalFormula.not(), solver);

      if (candCheckSol.unsat()) {
        // No counterexample
        return Optional.empty();
      }

      Instance cexInstance = candCheckSol.instance();

      HistoryFormula guidingFormula =
          this.cexEncoder.guide(cexInstance, checker.execution(), universalFormula, bounds);

      return Optional.of(
          new CegisFeedback<>(
              new Counterexample<>(cexInstance, checker.execution(), universalFormula),
              guidingFormula));
    }
  }

  private static <E extends Execution> List<ExecutionFormula<E>> naiveSearchFormula(
      SynthesisSpec<E> spec) {
    List<ExecutionFormula<E>> formulas = new ArrayList<>(spec.existentialFormulas());
    if (spec.hasUniversal()) {
      formulas.add(spec.universalFormula().get());
    }
    return formulas;
  }

  private static <E extends Execution> List<ExecutionFormula<E>> smartSearchFormula(
      SynthesisSpec<E> spec) {

    var univFormula = spec.universalFormula();

    // TODO: should probably be an explicit check
    assert !spec.existentialFormulas().isEmpty() || univFormula.isPresent();

    List<ExecutionFormula<E>> formulas = new ArrayList<>();
    if (spec.existentialFormulas().isEmpty()) {
      formulas.add(spec.universalFormula().get());
    } else {
      for (var formula : spec.existentialFormulas()) {
        formulas.add(univFormula.isPresent() ? formula.and(univFormula.get()) : formula);
      }
    }
    return formulas;
  }

  public <E extends Execution> List<E> register(
      SynthesisSpec<E> spec,
      ExecutionModuleConstructor<E, FolSynthesisInput, S, ?> encoderConstructor,
      CandidateChecker<E> checker,
      CounterexampleEncoder<E> counterexampleEncoder) {

    if (spec.hasUniversal()) {
      this.verifiers.add(
          new CegisVerifier<>(checker, counterexampleEncoder, spec.universalFormula().get()));
    }
    return this.synthesisEncoder.register(
        encoderConstructor, this.candidateSearchFormulaConstructor.construct(spec));
  }

  private CegisAggregatedFeedback guide(
      Instance instance, HistorySchema encoding, Bounds bounds, Solver solver) {
    List<Counterexample<?>> counterexamples = new ArrayList<>();
    HistoryFormula formula = h -> Formula.TRUE;
    for (var verifier : this.verifiers) {
      var maybeFeedback = verifier.verify(encoding, instance, bounds, solver);
      if (maybeFeedback.isPresent()) {
        var feedback = maybeFeedback.get();
        counterexamples.add(feedback.counterexample());
        formula = formula.and(feedback.guidingFormula());
      }
    }
    return new CegisAggregatedFeedback(counterexamples, formula);
  }

  public CegisResult synthesize(Options synthOptions, Options checkOptions) {
    return useIncrementalSolving
        ? synthesizeWithIncremental(synthOptions, checkOptions)
        : synthesizeWithoutIncremental(synthOptions, checkOptions);
  }

  public CegisResult synthesizeWithIncremental(Options synthOptions, Options checkOptions) {
    List<FailedCandidate> failedCandidates = new ArrayList<>();
    KodkodProblem searchProblem = this.synthesisEncoder.encode();

    IncrementalSolver synthesizer = IncrementalSolver.solver(synthOptions);
    Solver checker = new Solver(checkOptions);

    Instant start = Instant.now();

    Instant synthStart = Instant.now();
    Solution candSol = synthesizer.solve(searchProblem.formula(), searchProblem.bounds());
    long synthTime = Duration.between(synthStart, Instant.now()).toMillis();

    int firstSynthClauses = candSol.stats().clauses();

    if (candSol.unsat()) {
      return CegisResult.fail(
          historyEncoding(),
          failedCandidates,
          firstSynthClauses,
          firstSynthClauses,
          synthTime,
          0,
          Duration.between(start, Instant.now()).toMillis());
    }

    // check candidate
    Bounds newBounds = new Bounds(searchProblem.bounds().universe());
    Instant checkStart = Instant.now();
    CegisAggregatedFeedback feedback =
        guide(candSol.instance(), historyEncoding(), newBounds, checker);
    long checkTime = Duration.between(checkStart, Instant.now()).toMillis();

    while (!feedback.counterexamples().isEmpty()) {
      // Cooperative cancellation: bail between solves if interrupted. Best-effort — a native SAT
      // solve already in progress cannot be interrupted and will run to completion.
      if (Thread.interrupted()) {
        throw new CancellationException("CEGIS synthesis interrupted");
      }
      failedCandidates.add(new FailedCandidate(candSol.instance(), feedback.counterexamples()));

      synthStart = Instant.now();
      candSol = synthesizer.solve(feedback.guidingFormula().resolve(historyEncoding()), newBounds);
      synthTime += Duration.between(synthStart, Instant.now()).toMillis();

      if (candSol.unsat()) {
        // Stop if problem is UNSAT
        break;
      }
      // Otherwise, verify the new candidate
      newBounds = new Bounds(searchProblem.bounds().universe());
      checkStart = Instant.now();
      feedback = guide(candSol.instance(), historyEncoding(), newBounds, checker);
      checkTime += Duration.between(checkStart, Instant.now()).toMillis();
    }

    int totalSynthClauses = candSol.stats().clauses();
    long time = Duration.between(start, Instant.now()).toMillis();
    return feedback.counterexamples().isEmpty()
        ? CegisResult.success(
            historyEncoding(),
            candSol.instance(),
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

  public CegisResult synthesizeWithoutIncremental(Options synthOptions, Options checkOptions) {
    List<FailedCandidate> failedCandidates = new ArrayList<>();
    KodkodProblem searchProblem = this.synthesisEncoder.encode();

    Solver synthesizer = new Solver(synthOptions);
    Solver checker = new Solver(checkOptions);

    Instant start = Instant.now();

    Instant synthStart = Instant.now();
    Solution candSol = synthesizer.solve(searchProblem.formula(), searchProblem.bounds());
    long synthTime = Duration.between(synthStart, Instant.now()).toMillis();

    int firstSynthClauses = candSol.stats().clauses();

    if (candSol.unsat()) {
      return CegisResult.fail(
          historyEncoding(),
          failedCandidates,
          firstSynthClauses,
          firstSynthClauses,
          synthTime,
          0,
          Duration.between(start, Instant.now()).toMillis());
    }

    Instant checkStart = Instant.now();
    CegisAggregatedFeedback feedback =
        guide(candSol.instance(), historyEncoding(), searchProblem.bounds(), checker);
    long checkTime = Duration.between(checkStart, Instant.now()).toMillis();

    while (!feedback.counterexamples().isEmpty()) {
      // Cooperative cancellation: bail between solves if interrupted. Best-effort — a native SAT
      // solve already in progress cannot be interrupted and will run to completion.
      if (Thread.interrupted()) {
        throw new CancellationException("CEGIS synthesis interrupted");
      }
      failedCandidates.add(new FailedCandidate(candSol.instance(), feedback.counterexamples()));

      searchProblem = searchProblem.and(feedback.guidingFormula().resolve(historyEncoding()));

      synthStart = Instant.now();
      candSol = searchProblem.solve(synthesizer);
      synthTime += Duration.between(synthStart, Instant.now()).toMillis();

      if (candSol.unsat()) {
        // Stop if problem is UNSAT
        break;
      }
      // Otherwise, verify the new candidate
      checkStart = Instant.now();
      feedback = guide(candSol.instance(), historyEncoding(), searchProblem.bounds(), checker);
      checkTime += Duration.between(checkStart, Instant.now()).toMillis();
    }

    int totalSynthClauses = candSol.stats().clauses();
    long time = Duration.between(start, Instant.now()).toMillis();
    return feedback.counterexamples().isEmpty()
        ? CegisResult.success(
            historyEncoding(),
            candSol.instance(),
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
