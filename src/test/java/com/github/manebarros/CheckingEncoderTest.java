package com.github.manebarros;

import static com.github.manebarros.Operation.readOf;
import static com.github.manebarros.Operation.writeOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import org.junit.jupiter.api.Test;

public interface CheckingEncoderTest {
  CheckingEncoder encoder();

  @Test
  default void historyIsDisallowedByReadAtomic() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0), writeOf(1, 1))),
                    new Transaction(2, Arrays.asList(readOf(1, 0), writeOf(0, 1))))));
    Contextualized<KodkodProblem> p = encoder().encode(hist, AxiomaticDefinitions::ReadAtomic);
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void historyDoesNotHaveTapL() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0), writeOf(1, 1))),
                    new Transaction(2, Arrays.asList(readOf(1, 0), writeOf(0, 1))))));
    Contextualized<KodkodProblem> p = encoder().encode(hist, TransactionalAnomalousPatterns::l);
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }
}
