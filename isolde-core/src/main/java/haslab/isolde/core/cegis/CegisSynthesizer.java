package haslab.isolde.core.cegis;

import static haslab.isolde.biswas.definitions.HistoryOnlyIsolationCriterion.Causal;
import static haslab.isolde.biswas.definitions.IsolationCriterion.Prefix;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.cerone.definitions.CustomDefinitions;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import kodkod.engine.Evaluator;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
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
    if (r.sat()) {
      System.out.println(
          new Evaluator(r.instance())
              .evaluate(CustomDefinitions.versionOrders().resolve(historyEncoding())));
      System.out.println(
          new Evaluator(r.instance())
              .evaluate(CustomDefinitions.knowsAtLeast().resolve(historyEncoding())));
      System.out.println(
          "Causal mandatory commit order edges:\n"
              + new Evaluator(r.instance())
                  .evaluate(Causal.mandatoryCommitOrderEdges(historyEncoding())));
      System.out.println(
          "Prefix mandatory commit order edges:\n"
              + new Evaluator(r.instance())
                  .evaluate(
                      Prefix.mandatoryCommitOrderEdges(
                          new BiswasExecution(
                              historyEncoding(),
                              Causal.mandatoryCommitOrderEdges(historyEncoding())))));
      for (int i = 0; i < 4; i++) {

        System.out.println(
            "Prefix mandatory commit order edges depth "
                + i
                + ":\n"
                + new Evaluator(r.instance())
                    .evaluate(
                        CustomDefinitions.mandatoryCommitOrderEdgesPrefix(i)
                            .resolve(historyEncoding())));
      }
      // System.out.println(
      //    "Prefix mandatory commit order edges depth 2:\n"
      //        + new Evaluator(r.instance())
      //            .evaluate(
      //                Prefix.mandatoryCommitOrderEdges(
      //                    new BiswasExecution(
      //                        historyEncoding(),
      //                        Prefix.mandatoryCommitOrderEdges(
      //                            new BiswasExecution(
      //                                historyEncoding(),
      //                                Causal.mandatoryCommitOrderEdges(historyEncoding())))))));
    }
    return r.unsat() ? Optional.empty() : Optional.of(new History(historyEncoding(), r.instance()));
  }

  public List<Solution> synthesize() {
    List<Solution> candidates = new LinkedList<>();
    KodkodProblem searchProblem = this.synthesisEncoder.encode();

    IncrementalSolver synthesizer = IncrementalSolver.solver(new Options());
    Solver checker = new Solver();

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
      System.out.println(
          "cand "
              + ++candCount
              + " (after "
              + Duration.between(start, Instant.now()).toSeconds()
              + " s).");
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

  private AbstractHistoryK historyEncoding() {
    return this.synthesisEncoder.getHistoryEncoding();
  }
}
