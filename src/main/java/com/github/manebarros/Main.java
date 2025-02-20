package com.github.manebarros;

import static com.github.manebarros.Operation.readOf;
import static com.github.manebarros.Operation.writeOf;
import static java.util.Arrays.asList;

import java.util.Collections;
import kodkod.ast.Formula;
import kodkod.engine.Solution;

public class Main {
  public static void main(String[] args) {
    Scope scope = new Scope(3, 1, 3, 1);
    CegisSynthesizer synth =
        new CegisSynthesizer(DirectSynthesisEncoder.instance(), DirectCheckingEncoder.instance());

    BiswasExecutionFormula formula =
        (h, co) -> TransactionalAnomalousPatterns.n(h, h.mandatoryCommitOrderEdgesCC()).not();
    History raViolation =
        new History(
            Collections.singletonList(
                new Session(
                    asList(
                        new Transaction(1, asList(readOf(0, 0), writeOf(0, 1))),
                        new Transaction(2, asList(readOf(0, 1), writeOf(0, 2))),
                        new Transaction(3, asList(readOf(0, 1)))))));
    //
    //    History hist =
    //        new History(
    //            Collections.singletonList(
    //                new Session(
    //                    asList(
    //                        new Transaction(
    //                            1, asList(readOf(0, 0), readOf(1, 0), writeOf(0, 1), writeOf(1,
    // 1))),
    //                        new Transaction(2, asList(readOf(0, 1), writeOf(1, 2))),
    //                        new Transaction(3, asList(readOf(1, 1), writeOf(0, 2)))))));
    //
    //    History causalityViolation =
    //        new History(
    //            Arrays.asList(
    //                new Session(new Transaction(1, asList(readOf(0, 0), writeOf(0, 1)))),
    //                new Session(new Transaction(2, asList(readOf(0, 1), writeOf(1, 1)))),
    //                new Session(new Transaction(3, asList(readOf(1, 1), readOf(0, 0))))));
    //
    //    History causalityViolation2 =
    //        new History(
    //            Arrays.asList(
    //                new Session(
    //                    new Transaction(
    //                        1, asList(readOf(0, 0), readOf(1, 0), writeOf(0, 1), writeOf(1, 1)))),
    //                new Session(
    //                    Arrays.asList(
    //                        new Transaction(2, asList(readOf(1, 0), writeOf(1, 2))),
    //                        new Transaction(3, asList(readOf(0, 0), readOf(1, 2), writeOf(0, 2))),
    //                        new Transaction(4, asList(readOf(0, 1), readOf(1, 2)))))));

    // Contextualized<Solution> sol =
    //    synth.synthesize(
    //        scope,
    //        Collections.singletonList(formula),
    //        (h, co) -> AxiomaticDefinitions.Causal(h, co).not());

    Contextualized<Solution> sol =
        synth.synthesize(
            scope,
            Collections.singletonList(
                (h, co) ->
                    FormulaUtil.equivalentToHistory(raViolation)
                        .apply(h, co)
                        .and(TransactionalAnomalousPatterns.l(h, co))),
            (h, co) -> Formula.TRUE);

    if (sol.getContent().unsat()) {
      System.out.println("not sat");
    } else {
      History h = new History(sol.getHistoryEncoding(), sol.getContent().instance());
      // System.out.println(sol.getContent().instance());
      System.out.println(h);
      // System.out.println(
      //    new Evaluator(sol.getContent().instance())
      //        .evaluate(DirectAbstractHistoryEncoding.instance().mandatoryCommitOrderEdgesCC()));
    }
  }
}
