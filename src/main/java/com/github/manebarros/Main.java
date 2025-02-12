package com.github.manebarros;

import java.util.Arrays;
import kodkod.ast.Formula;
import kodkod.engine.Solution;

public class Main {
  public static void main(String[] args) {
    Scope scope = new Scope(3, 2, 3, 2);
    CegisSynthesizer synth =
        new CegisSynthesizer(DirectSynthesisEncoder.instance(), DirectCheckingEncoder.instance());
    Contextualized<Solution> sol =
        synth.synthesize(
            scope,
            Arrays.asList(AxiomaticDefinitions::ReadAtomic, TransactionalAnomalousPatterns::l),
            (h, co) -> Formula.TRUE);

    // Contextualized<Solution> sol =
    //    synth.synthesize(
    //        scope,
    //        Collections.singletonList((h, co) -> Formula.TRUE),
    //        (h, co) ->
    //            TransactionalAnomalousPatterns.l(h, co)
    //                .not()
    //                .and(AxiomaticDefinitions.ReadAtomic(h, co).not()));
    // History hist =
    //    new History(
    //        Collections.singletonList(
    //            new Session(
    //                asList(
    //                    new Transaction(
    //                        1, asList(readOf(0, 0), readOf(1, 0), writeOf(0, 1), writeOf(1, 1))),
    //                    new Transaction(2, asList(readOf(0, 1), writeOf(1, 2))),
    //                    new Transaction(3, asList(readOf(1, 1), writeOf(0, 2)))))));
    if (sol.getContent().unsat()) {
      System.out.println("not sat");
    } else {
      History h = new History(sol.getHistoryEncoding(), sol.getContent().instance());
      // System.out.println(sol.getContent().instance());
      System.out.println(h);
      //      System.out.println(
      //          new Evaluator(sol.getContent().instance())
      //              .evaluate(DirectAbstractHistoryEncoding.instance().binaryWr()));
    }
  }
}
