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
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public class CegisSynthesizer {
  private final SynthesisEncoder synthesisEncoder;
  private final CheckingEncoder<CeroneExecutionK> ceroneCheckingEncoder;
  private final CheckingEncoder<BiswasExecutionK> biswasCheckingEncoder;

  public CegisSynthesizer(
      SynthesisEncoder synthesisEncoder,
      CheckingEncoder<CeroneExecutionK> ceroneCheckingEncoder,
      CheckingEncoder<BiswasExecutionK> biswasCheckingEncoder) {
    this.synthesisEncoder = synthesisEncoder;
    this.ceroneCheckingEncoder = ceroneCheckingEncoder;
    this.biswasCheckingEncoder = biswasCheckingEncoder;
  }

  private <E extends DatabaseExecution> List<ExecutionFormulaK<E>> calculateSearchFormula(
      SynthesisSpec<E> spec) {
    return calculateSearchFormula(spec.getExistentialConstraints(), spec.getUniversalConstraint());
  }

  private <E extends DatabaseExecution> List<ExecutionFormulaK<E>> calculateSearchFormula(
      List<ExecutionFormulaK<E>> existentialFormulas, ExecutionFormulaK<E> universalFormula) {
    List<ExecutionFormulaK<E>> formulas = new ArrayList<>();
    if (existentialFormulas.isEmpty()) {
      formulas.add(universalFormula);
    } else {
      for (var formula : existentialFormulas) {
        formulas.add(formula.and(universalFormula));
      }
    }
    return formulas;
  }

  public Contextualized<Solution> synthesize(
      Scope scope,
      SynthesisSpec<BiswasExecutionK> biswasSpec,
      SynthesisSpec<CeroneExecutionK> ceroneSpec) {

    List<ExecutionFormulaK<BiswasExecutionK>> biswasSearchFormulas =
        calculateSearchFormula(biswasSpec);
    List<ExecutionFormulaK<CeroneExecutionK>> ceroneSearchFormulas =
        calculateSearchFormula(ceroneSpec);

    Contextualized<KodkodProblem> searchProblem =
        this.synthesisEncoder.encode(scope, biswasSearchFormulas, ceroneSearchFormulas);

    IncrementalSolver synthesizer = IncrementalSolver.solver(new Options());
    Solver checker = new Solver();

    Solution candSol =
        synthesizer.solve(
            searchProblem.getContent().formula(), searchProblem.getContent().bounds());

    if (candSol.unsat()) {
      return searchProblem.replace(candSol);
    }

    Contextualized<Solution> candBiswasCheckSol =
        this.biswasCheckingEncoder
            .encode(
                searchProblem.getHistoryEncoding(),
                candSol.instance(),
                biswasSpec.getUniversalConstraint().not())
            .fmap(p -> checker.solve(p.formula(), p.bounds()));

    Contextualized<Solution> candCeroneCheckSol =
        this.ceroneCheckingEncoder
            .encode(
                searchProblem.getHistoryEncoding(),
                candSol.instance(),
                ceroneSpec.getUniversalConstraint().not())
            .fmap(p -> checker.solve(p.formula(), p.bounds()));

    while ((candBiswasCheckSol.getContent().sat() || candCeroneCheckSol.getContent().sat())
        && (candSol =
                nextCandidate(
                    synthesizer,
                    candBiswasCheckSol,
                    candCeroneCheckSol,
                    searchProblem.getHistoryEncoding(),
                    searchProblem.getContent().bounds().universe(),
                    biswasSpec.getUniversalConstraint(),
                    ceroneSpec.getUniversalConstraint()))
            .sat()) {
      candBiswasCheckSol =
          this.biswasCheckingEncoder
              .encode(
                  searchProblem.getHistoryEncoding(),
                  candSol.instance(),
                  biswasSpec.getUniversalConstraint().not())
              .fmap(p -> checker.solve(p.formula(), p.bounds()));

      candCeroneCheckSol =
          this.ceroneCheckingEncoder
              .encode(
                  searchProblem.getHistoryEncoding(),
                  candSol.instance(),
                  ceroneSpec.getUniversalConstraint().not())
              .fmap(p -> checker.solve(p.formula(), p.bounds()));
    }
    return searchProblem.replace(candSol);
  }

  private Solution nextCandidate(
      IncrementalSolver solver,
      Contextualized<Solution> biswasCex,
      Contextualized<Solution> ceroneCex,
      AbstractHistoryK historyEncoding,
      Universe synthesisUniverse,
      ExecutionFormulaK<BiswasExecutionK> biswasUnivFormula,
      ExecutionFormulaK<CeroneExecutionK> ceroneUnivFormula) {
    Formula f = Formula.TRUE;
    Bounds b = new Bounds(synthesisUniverse);

    if (biswasCex.getContent().sat()) {
      TupleSet commitOrderVal =
          new Evaluator(biswasCex.getContent().instance())
              .evaluate(biswasCex.getBiswasExecutions().get(0).commitOrder());
      Relation cexCommitOrderRel = Relation.binary("cexCommitOrder");
      b.boundExactly(cexCommitOrderRel, commitOrderVal);
      f =
          f.and(
              biswasUnivFormula.apply(
                  new DefaultBiswasExecutionK(historyEncoding, cexCommitOrderRel)));
    }

    if (ceroneCex.getContent().sat()) {
      var eval = new Evaluator(ceroneCex.getContent().instance());
      TupleSet visVal = eval.evaluate(ceroneCex.getCeroneExecutions().get(0).vis());
      TupleSet arVal = eval.evaluate(ceroneCex.getCeroneExecutions().get(0).ar());
      Relation cexVisRel = Relation.binary("cex vis");
      Relation cexArRel = Relation.binary("cex ar");
      b.boundExactly(cexVisRel, visVal);
      b.boundExactly(cexArRel, arVal);
      f =
          f.and(
              ceroneUnivFormula.apply(
                  new DefaultCeroneExecutionK(historyEncoding, cexVisRel, cexArRel)));
    }

    return solver.solve(f, b);
  }

  public SynthesisEncoder getSynthesisEncoder() {
    return synthesisEncoder;
  }

  public CheckingEncoder<CeroneExecutionK> getCeroneCheckingEncoder() {
    return ceroneCheckingEncoder;
  }

  public CheckingEncoder<BiswasExecutionK> getBiswasCheckingEncoder() {
    return biswasCheckingEncoder;
  }
}
