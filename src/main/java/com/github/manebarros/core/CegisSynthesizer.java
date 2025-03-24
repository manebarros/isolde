package com.github.manebarros.core;

import com.github.manebarros.kodkod.KodkodProblem;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public class CegisSynthesizer {
  private final FolSynthesisEncoder synthesisEncoder;
  private final List<CegisVerifier<?>> checkingEncoders;

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
      CheckingEncoder<E> checkingEncoder,
      CounterexampleEncoder<E> counterexampleEncoder) {
    this.checkingEncoders.add(
        new CegisVerifier<>(checkingEncoder, counterexampleEncoder, spec.universalFormula()));
    SynthesisModule<E> module =
        synthesisEncoder.encode(this.synthesisEncoder, calculateSearchFormula(spec));
    this.synthesisEncoder.register(module);
    return new CegisModule<E>(module.executions(), checkingEncoder.execution());
  }

  private record CegisVerifier<E extends Execution>(
      CheckingEncoder<E> checkingEncoder,
      CounterexampleEncoder<E> cexEncoder,
      ExecutionFormula<E> universalFormula) {

    Optional<HistoryFormula> guide(
        AbstractHistoryK history, Instance instance, Bounds bounds, Solver solver) {
      Solution candCheckSol =
          checkingEncoder.encode(history, instance, this.universalFormula.not()).solve(solver);
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

    Optional<HistoryFormula> of;
    Bounds newBounds = new Bounds(searchProblem.bounds().universe());
    while ((of =
                guide(
                    this.synthesisEncoder.getHistoryEncoding(),
                    candSol.instance(),
                    newBounds,
                    checker))
            .isPresent()
        && (candSol =
                synthesizer.solve(
                    of.get().resolve(this.synthesisEncoder.getHistoryEncoding()), newBounds))
            .sat()) {
      candidates.addFirst(candSol);
    }
    if (candSol.unsat()) {
      candidates.addFirst(candSol);
    }
    return candidates;
  }
}
