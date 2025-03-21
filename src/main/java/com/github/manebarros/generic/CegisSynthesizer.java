package com.github.manebarros.generic;

import com.github.manebarros.KodkodProblem;
import com.github.manebarros.Scope;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kodkod.ast.Formula;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;

public class CegisSynthesizer {
  private final FolSynthesisEncoder synthesisEncoder;
  private final List<CegisVerifier<?>> checkingEncoders;

  public CegisSynthesizer(Scope scope, Formula historyFormula) {
    this.synthesisEncoder = new FolSynthesisEncoder(scope, historyFormula);
    this.checkingEncoders = new ArrayList<>();
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

  public <E extends Execution> List<E> add(
      List<ExecutionFormula<E>> existentialFormulas,
      ExecutionFormula<E> universalFormula,
      SynthesisEncoder<E> synthesisEncoder,
      CheckingEncoder<E> checkingEncoder,
      CounterexampleEncoder<E> counterexampleEncoder) {
    this.checkingEncoders.add(
        new CegisVerifier<>(checkingEncoder, counterexampleEncoder, universalFormula));
    return this.synthesisEncoder.add(
        calculateSearchFormula(existentialFormulas, universalFormula), synthesisEncoder);
  }

  private record CegisVerifier<E extends Execution>(
      CheckingEncoder<E> checkingEncoder,
      CounterexampleEncoder<E> cexEncoder,
      ExecutionFormula<E> universalFormula) {

    Optional<Formula> guide(Instance instance, Bounds bounds, Solver solver) {
      Solution candCheckSol =
          checkingEncoder.encode(instance, this.universalFormula.not()).solve(solver);
      if (candCheckSol.unsat()) {
        // No counterexample
        return Optional.empty();
      }
      return Optional.of(
          this.cexEncoder.guide(candCheckSol.instance(), this.checkingEncoder.execution(), bounds));
    }
  }

  private Optional<Formula> guide(Instance instance, Bounds bounds, Solver solver) {
    Formula r = null;
    for (var verifier : this.checkingEncoders) {
      Optional<Formula> f = verifier.guide(instance, bounds, solver);
      if (f.isPresent()) {
        r = r == null ? f.get() : r.and(f.get());
      }
    }
    return r == null ? Optional.empty() : Optional.of(r);
  }

  public Solution synthesize() {
    KodkodProblem searchProblem = this.synthesisEncoder.encode();

    IncrementalSolver synthesizer = IncrementalSolver.solver(new Options());
    Solver checker = new Solver();

    Solution candSol = synthesizer.solve(searchProblem.formula(), searchProblem.bounds());

    if (candSol.unsat()) {
      return candSol;
    }

    Optional<Formula> of;
    Bounds newBounds = new Bounds(searchProblem.bounds().universe());
    while ((of = guide(candSol.instance(), newBounds, checker)).isPresent()
        && (candSol = synthesizer.solve(of.get(), newBounds)).sat()) {}
    return candSol;
  }
}
