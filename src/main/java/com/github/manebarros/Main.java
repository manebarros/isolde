package com.github.manebarros;

import static kodkod.ast.Formula.TRUE;

import java.util.Collections;
import kodkod.engine.Evaluator;
import kodkod.engine.Solution;

public class Main {
  public static void main(String[] args) {
    Scope scope = new Scope(1, 3, 3, 3, 3);
    Synthesizer solver = new Synthesizer();
    Solution sol =
        solver.synthesize(
            scope,
            Collections.singletonList(c -> AxiomaticDefinitions.cutIsolation(c).not()),
            c -> TRUE);
    if (sol.unsat()) {
      System.out.println("not sat");
    } else {
      History h = new History(sol.instance());
      System.out.println(h);
      // System.out.println(sol.instance());
      System.out.println(new Evaluator(sol.instance()).evaluate(HistoryEncoding.wr()));
    }
  }
}
