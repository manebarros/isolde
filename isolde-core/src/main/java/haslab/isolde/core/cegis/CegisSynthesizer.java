package haslab.isolde.core.cegis;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.check.DefaultHistoryCheckingEncoder;
import haslab.isolde.core.check.candidate.CandCheckEncoder;
import haslab.isolde.core.check.candidate.CandCheckHistoryEncoder;
import haslab.isolde.core.check.candidate.CandCheckModuleEncoderConstructor;
import haslab.isolde.core.synth.FolSynthesisEncoder;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.core.synth.SynthesisModuleEncoder;
import haslab.isolde.history.History;
import haslab.isolde.kodkod.KodkodProblem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public class CegisSynthesizer {
  private final FolSynthesisEncoder synthesisEncoder;
  private final List<CegisVerifier<?>> checkingEncoders;

  private final CandCheckHistoryEncoder candCheckHistoryEncoder =
      DefaultHistoryCheckingEncoder.instance();

  public CegisSynthesizer(Scope scope) {
    this.synthesisEncoder = new FolSynthesisEncoder(scope);
    this.checkingEncoders = new ArrayList<>();
  }

  public CegisSynthesizer(Scope scope, HistoryFormula historyFormula) {
    this.synthesisEncoder = new FolSynthesisEncoder(scope, historyFormula);
    this.checkingEncoders = new ArrayList<>();
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

  public record CegisModule<E extends Execution>(
      List<E> synthesisExecutions, E checkingExecution) {}

  public <E extends Execution> CegisModule<E> add(
      SynthesisSpec<E> spec,
      SynthesisModuleEncoder<E> synthesisEncoder,
      CandCheckEncoder<E> checkingEncoder,
      CounterexampleEncoder<E> counterexampleEncoder) {
    this.checkingEncoders.add(
        new CegisVerifier<>(checkingEncoder, counterexampleEncoder, spec.universalFormula()));
    return new CegisModule<E>(
        this.synthesisEncoder.register(synthesisEncoder, calculateSearchFormula(spec)),
        checkingEncoder.execution());
  }

  public <E extends Execution> CegisModule<E> add(
      SynthesisSpec<E> spec,
      SynthesisModuleEncoder<E> synthesisEncoder,
      CandCheckModuleEncoderConstructor<E> moduleEncoderGenerator,
      CounterexampleEncoder<E> counterexampleEncoder) {
    return add(
        spec,
        synthesisEncoder,
        new CandCheckEncoder<>(candCheckHistoryEncoder, moduleEncoderGenerator),
        counterexampleEncoder);
  }

  private record CegisVerifier<E extends Execution>(
      CandCheckEncoder<E> checkingEncoder,
      CounterexampleEncoder<E> cexEncoder,
      ExecutionFormula<E> universalFormula) {

    Optional<HistoryFormula> guide(
        AbstractHistoryK history, Instance instance, Bounds bounds, Solver solver) {
      Solution candCheckSol =
          checkingEncoder.solve(instance, history, universalFormula.not(), solver);

      if (candCheckSol.unsat()) {
        // No counterexample
        return Optional.empty();
      }
      return Optional.of(
          this.cexEncoder.guide(
              candCheckSol.instance(),
              this.checkingEncoder.execution(),
              this.universalFormula,
              bounds));
    }
  }

  private Optional<HistoryFormula> guide(
      AbstractHistoryK history, Instance instance, Bounds bounds, Solver solver) {
    HistoryFormula r = null;
    for (var verifier : this.checkingEncoders) {
      Optional<HistoryFormula> f = verifier.guide(history, instance, bounds, solver);
      if (f.isPresent()) {
        r = r == null ? f.get() : r.and(f.get());
      }
    }
    return r == null ? Optional.empty() : Optional.of(r);
  }

  public Optional<History> synthesizeH() {
    Solution r = synthesize().getFirst();
    return r.unsat() ? Optional.empty() : Optional.of(new History(historyEncoding(), r.instance()));
  }

  public List<Solution> synthesize(Options synthOptions, Options checkOptions) {
    List<Solution> candidates = new LinkedList<>();
    KodkodProblem searchProblem = this.synthesisEncoder.encode();

    IncrementalSolver synthesizer = IncrementalSolver.solver(synthOptions);
    Solver checker = new Solver(checkOptions);

    Solution candSol = synthesizer.solve(searchProblem.formula(), searchProblem.bounds());
    candidates.addFirst(candSol);

    if (candSol.unsat()) {
      return candidates;
    }

    Instant start = Instant.now();
    int candCount = 1;
    Bounds newBounds = new Bounds(searchProblem.bounds().universe());
    Optional<HistoryFormula> feedback =
        guide(historyEncoding(), candSol.instance(), newBounds, checker);
    while (feedback.isPresent()) {
      candSol = synthesizer.solve(feedback.get().resolve(historyEncoding()), newBounds);
      // System.out.println(
      //    "cand "
      //        + ++candCount
      //        + " (after "
      //        + Duration.between(start, Instant.now()).toSeconds()
      //        + " s).");
      candidates.addFirst(candSol); // Register new (potential) candidate
      if (candSol.unsat()) {
        // Stop if problem is UNSAT
        break;
      }
      // Otherwise, verify the new candidate
      newBounds = new Bounds(searchProblem.bounds().universe());
      feedback = guide(historyEncoding(), candSol.instance(), newBounds, checker);
    }
    return candidates;
  }

  public List<Solution> synthesize() {
    return synthesize(new Options());
  }

  public List<Solution> synthesize(SATFactory satSolver) {
    Options opt = new Options();
    opt.setSolver(satSolver);
    return synthesize(opt);
  }

  public List<Solution> synthesize(Options options) {
    return synthesize(options, options);
  }

  private AbstractHistoryK historyEncoding() {
    return this.synthesisEncoder.getHistoryEncoding();
  }
}
