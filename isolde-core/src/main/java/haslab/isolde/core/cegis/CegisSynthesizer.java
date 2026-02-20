package haslab.isolde.core.cegis;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.CegisResult.Counterexample;
import haslab.isolde.core.cegis.CegisResult.FailedCandidate;
import haslab.isolde.core.check.candidate.CandCheckerI;
import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.HistoryConstraintProblem;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.history.History;
import haslab.isolde.kodkod.FormulaUtil;
import haslab.isolde.kodkod.KodkodProblem;
import haslab.isolde.kodkod.Util;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
  private final List<CegisVerifier<?>> checkingEncoders;
  private final CandidateSearchFormulaConstructor candidateSearchFormulaConstructor;
  private final boolean useIncrementalSolving;

  private CegisSynthesizer(
      HistoryConstraintProblem<FolSynthesisInput, T, S> synthesisEncoder,
      CandidateSearchFormulaConstructor searchFormulaConstructor,
      boolean useIncrementalSolving) {
    this.synthesisEncoder = synthesisEncoder;
    this.checkingEncoders = new ArrayList<>();
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
      CandCheckerI<E> checkingEncoder,
      CounterexampleEncoder<E> cexEncoder,
      ExecutionFormula<E> universalFormula) {

    Optional<CegisFeedback<E>> verify(
        AbstractHistoryK history, Instance instance, Bounds bounds, Solver solver) {

      Solution candCheckSol =
          checkingEncoder.check(instance, history, universalFormula.not(), solver);

      if (candCheckSol.unsat()) {
        // No counterexample
        return Optional.empty();
      }

      Instance cexInstance = candCheckSol.instance();

      HistoryFormula guidingFormula =
          this.cexEncoder.guide(cexInstance, checkingEncoder.execution(), universalFormula, bounds);

      return Optional.of(
          new CegisFeedback<>(
              new Counterexample<>(cexInstance, checkingEncoder.execution(), universalFormula),
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
      CandCheckerI<E> checkingEncoder,
      CounterexampleEncoder<E> counterexampleEncoder) {

    if (spec.hasUniversal()) {
      this.checkingEncoders.add(
          new CegisVerifier<>(
              checkingEncoder, counterexampleEncoder, spec.universalFormula().get()));
    }
    return this.synthesisEncoder.register(
        encoderConstructor, this.candidateSearchFormulaConstructor.construct(spec));
  }

  private CegisAggregatedFeedback guide(
      Instance instance, AbstractHistoryK encoding, Bounds bounds, Solver solver) {
    List<Counterexample<?>> counterexamples = new ArrayList<>();
    HistoryFormula formula = h -> Formula.TRUE;
    for (var verifier : this.checkingEncoders) {
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

  public AbstractHistoryK historyEncoding() {
    return this.synthesisEncoder.historyEncoding();
  }

  public static record Status<E extends Execution>(
      int cand,
      Instance candidate,
      Instance counterexample,
      Formula buggyFormula,
      Bounds bounds,
      AbstractHistoryK historyEncoding) {

    @Override
    public final String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Failed with candidate ").append(cand);
      sb.append("Candidate instance:\n")
          .append(new History(historyEncoding, candidate))
          .append("\n\n");
      sb.append("Cex instance:\n")
          .append(new History(historyEncoding, counterexample))
          .append("\n\n\n\n");
      sb.append("Raw cand instance:\n").append(candidate).append("\n\n");
      sb.append("Raw cex instance:\n").append(counterexample).append("\n\n");
      sb.append("Formula:\n").append(buggyFormula).append("\n\n");
      sb.append("Failing bounds:\n").append(bounds).append("\n\n");
      return sb.toString();
    }
  }

  public Optional<Status<?>> identify(Options synthOptions, Options checkOptions, History history) {
    HistoryFormula historyFormula = FormulaUtil.equivalentToHistory(history);
    List<FailedCandidate> failedCandidates = new ArrayList<>();

    KodkodProblem searchProblem = this.synthesisEncoder.encode();
    IncrementalSolver synthesizer = IncrementalSolver.solver(synthOptions);

    KodkodProblem debugProblem = searchProblem.clone();
    Solver debugSynth = new Solver(synthOptions);

    Solver checker = new Solver(checkOptions);

    Solution candSol = searchProblem.solve(synthesizer);
    int candCount = 0;

    if (candSol.unsat()) {
      return null; // the problem itself is UNSAT
    }

    Solution debugSol =
        debugProblem.and(historyFormula.resolve(historyEncoding())).solve(debugSynth);
    if (debugSol.unsat()) {
      return Optional.of(
          new Status<>(
              candCount, null, null, null, null, historyEncoding())); // fails before any candidate
    }

    Bounds newBounds = new Bounds(searchProblem.bounds().universe());
    CegisAggregatedFeedback feedback =
        guide(candSol.instance(), historyEncoding(), newBounds, checker);
    while (!feedback.counterexamples().isEmpty()) {
      failedCandidates.add(new FailedCandidate(candSol.instance(), feedback.counterexamples()));

      // got some counterexamples. let's analyze them
      // assume a single universal formula
      candCount++;
      Util.extend(debugProblem.bounds(), newBounds);
      assert debugProblem.and(historyFormula.resolve(historyEncoding())).solve(debugSynth).sat();
      Solution wrongSol =
          debugProblem
              .and(historyFormula.resolve(historyEncoding()))
              .and(feedback.guidingFormula().resolve(historyEncoding()).not())
              .solve(debugSynth);
      assert wrongSol.sat();
      debugProblem = debugProblem.and(feedback.guidingFormula().resolve(historyEncoding()));
      debugSol = debugProblem.and(historyFormula.resolve(historyEncoding())).solve(debugSynth);

      if (debugSol.unsat()) {
        Optional<Status<?>> r =
            Optional.of(
                new Status<>(
                    candCount,
                    candSol.instance(),
                    feedback.counterexamples().get(0).instance(),
                    feedback.guidingFormula().resolve(historyEncoding()),
                    debugProblem.bounds(),
                    historyEncoding()));
        return r;
      }
      candSol = synthesizer.solve(feedback.guidingFormula().resolve(historyEncoding()), newBounds);

      if (candSol.unsat()) {
        // Stop if problem is UNSAT
        break;
      }
      // Otherwise, verify the new candidate
      newBounds = new Bounds(searchProblem.bounds().universe());
      feedback = guide(candSol.instance(), historyEncoding(), newBounds, checker);
    }

    return Optional.empty();
  }
}
