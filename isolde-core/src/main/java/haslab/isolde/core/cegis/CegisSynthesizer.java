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
  private final HistoryConstraintProblem<FolSynthesisInput, T, S> synthesisEncoder;
  private final List<CegisVerifier<?>> checkingEncoders;

  public CegisSynthesizer(HistoryConstraintProblem<FolSynthesisInput, T, S> synthesisEncoder) {
    this.synthesisEncoder = synthesisEncoder;
    this.checkingEncoders = new ArrayList<>();
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

  private <E extends Execution> List<ExecutionFormula<E>> calculateSearchFormula(
      SynthesisSpec<E> spec) {
    return calculateSearchFormula(spec.existentialFormulas(), spec.universalFormula());
  }

  private <E extends Execution> List<ExecutionFormula<E>> calculateSearchFormula(
      List<ExecutionFormula<E>> existentialFormulas, ExecutionFormula<E> universalFormula) {
    List<ExecutionFormula<E>> formulas = new ArrayList<>();
    if (existentialFormulas.isEmpty()) {
      formulas.add(universalFormula);
    } else {
      for (var formula : existentialFormulas) {
        formulas.add(formula.and(universalFormula));
      }
    }
    return formulas;
  }

  public <E extends Execution> List<E> register(
      SynthesisSpec<E> spec,
      ExecutionModuleConstructor<E, FolSynthesisInput, S, ?> encoderConstructor,
      CandCheckerI<E> checkingEncoder,
      CounterexampleEncoder<E> counterexampleEncoder) {
    this.checkingEncoders.add(
        new CegisVerifier<>(checkingEncoder, counterexampleEncoder, spec.universalFormula()));
    return this.synthesisEncoder.register(encoderConstructor, calculateSearchFormula(spec));
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
    List<FailedCandidate> failedCandidates = new ArrayList<>();
    KodkodProblem searchProblem = this.synthesisEncoder.encode();

    IncrementalSolver synthesizer = IncrementalSolver.solver(synthOptions);
    Solver checker = new Solver(checkOptions);

    Instant start = Instant.now();

    Solution candSol = synthesizer.solve(searchProblem.formula(), searchProblem.bounds());

    if (candSol.unsat()) {
      return CegisResult.fail(
          historyEncoding(), failedCandidates, Duration.between(start, Instant.now()).toMillis());
    }

    Bounds newBounds = new Bounds(searchProblem.bounds().universe());
    CegisAggregatedFeedback feedback =
        guide(candSol.instance(), historyEncoding(), newBounds, checker);
    while (!feedback.counterexamples().isEmpty()) {
      failedCandidates.add(new FailedCandidate(candSol.instance(), feedback.counterexamples()));
      candSol = synthesizer.solve(feedback.guidingFormula().resolve(historyEncoding()), newBounds);

      if (candSol.unsat()) {
        // Stop if problem is UNSAT
        break;
      }
      // Otherwise, verify the new candidate
      newBounds = new Bounds(searchProblem.bounds().universe());
      feedback = guide(candSol.instance(), historyEncoding(), newBounds, checker);
    }

    long time = Duration.between(start, Instant.now()).toMillis();
    return feedback.counterexamples().isEmpty()
        ? CegisResult.success(historyEncoding(), candSol.instance(), failedCandidates, time)
        : CegisResult.fail(historyEncoding(), failedCandidates, time);
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

    Instant start = Instant.now();

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

    long time = Duration.between(start, Instant.now()).toMillis();
    return Optional.empty();
  }
}
