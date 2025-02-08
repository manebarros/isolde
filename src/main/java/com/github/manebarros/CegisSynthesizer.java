package com.github.manebarros;

import java.util.ArrayList;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

public class CegisSynthesizer {
  private final SynthesisEncoder synthesisEncoder;
  private final CheckingEncoder checkingEncoder;

  public CegisSynthesizer(SynthesisEncoder synthesisEncoder, CheckingEncoder checkingEncoder) {
    this.synthesisEncoder = synthesisEncoder;
    this.checkingEncoder = checkingEncoder;
  }

  private List<ExecutionFormulaG> calculateSearchFormula(
      List<ExecutionFormulaG> existentialFormulas, ExecutionFormulaG universalFormula) {
    List<ExecutionFormulaG> formulas = new ArrayList<>();
    if (existentialFormulas.isEmpty()) {
      formulas.add(universalFormula);
    } else {
      for (var formula : existentialFormulas) {
        formulas.add((h, co) -> formula.apply(h, co).and(universalFormula.apply(h, co)));
      }
    }
    return formulas;
  }

  public Solution synthesize(
      Scope scope,
      List<ExecutionFormulaG> existentialFormulas,
      ExecutionFormulaG universalFormula) {

    List<ExecutionFormulaG> searchFormulas =
        calculateSearchFormula(existentialFormulas, universalFormula);

    Contextualized<KodkodProblem> searchProblem =
        this.synthesisEncoder.encode(scope, searchFormulas);

    IncrementalSolver synthesizer = IncrementalSolver.solver(new Options());
    Solver checker = new Solver();

    Solution candSol =
        synthesizer.solve(
            searchProblem.getContent().formula(), searchProblem.getContent().bounds());

    if (candSol.unsat()) {
      return candSol;
    }

    Contextualized<Solution> candCheckSol =
        this.checkingEncoder
            .encode(searchProblem.getHistoryEncoding(), candSol.instance(), universalFormula.not())
            .fmap(p -> checker.solve(p.formula(), p.bounds()));

    while (candCheckSol.getContent().sat()
        && (candSol =
                nextCandidate(
                    synthesizer,
                    candCheckSol.fmap(Solution::instance),
                    searchProblem.getHistoryEncoding(),
                    universalFormula))
            .sat()) {
      candCheckSol =
          this.checkingEncoder
              .encode(
                  searchProblem.getHistoryEncoding(), candSol.instance(), universalFormula.not())
              .fmap(p -> checker.solve(p.formula(), p.bounds()));
    }
    return candSol;
  }

  private Solution nextCandidate(
      IncrementalSolver solver,
      Contextualized<Instance> counterexample,
      AbstractHistoryK historyEncoding,
      ExecutionFormulaG universalFormula) {
    TupleSet commitOrderVal =
        new Evaluator(counterexample.getContent())
            .evaluate(counterexample.getCommitOrders().get(0));
    Relation cexCommitOrderRel = Relation.binary("cexCommitOrder");
    Bounds b =
        new Bounds(
            commitOrderVal
                .universe()); // WARN: Change if the universe of checking becomes different from the
    // one for synthesis
    b.boundExactly(cexCommitOrderRel, commitOrderVal);
    Formula f = universalFormula.apply(historyEncoding, cexCommitOrderRel);
    return solver.solve(f, b);
  }
}
