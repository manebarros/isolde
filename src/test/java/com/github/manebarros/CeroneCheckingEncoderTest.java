package com.github.manebarros;

import static com.github.manebarros.Operation.readOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import org.junit.jupiter.api.Test;

/**
 * The following properties must be true for every instance when checking histories:
 *
 * <ul>
 *   <li>`ar` is a total order over transactions
 *   <li>`vis` \subseteq `ar`
 *   <li>`so` \not\subseteq `vis` is possible
 *   <li>`so` \subseteq `ar`
 *   <li>The initial txn precedes all others in `vis`
 *   <li>The initial txn precedes all others in `so`
 *   <li>The initial txn precedes all others in `ar`
 * </ul>
 */
public interface CeroneCheckingEncoderTest {
  CheckingEncoder<CeroneExecutionK> encoder();

  @Test
  default void arbitrationTotallyOrdersTransactions() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<CeroneExecutionK, KodkodProblem> p =
        encoder()
            .encode(hist, (h, co) -> KodkodUtil.strictTotalOrder(co.ar(), h.transactions()).not());
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void visSubsetOfAr() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<CeroneExecutionK, KodkodProblem> p =
        encoder().encode(hist, (h, co) -> co.vis().in(co.ar()).not());
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void soMightNotBeSubsetOfVis() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<CeroneExecutionK, KodkodProblem> p =
        encoder().encode(hist, (h, co) -> h.sessionOrder().in(co.vis()).not());
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.sat());
  }

  @Test
  default void soSubsetOfAr() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<CeroneExecutionK, KodkodProblem> p =
        encoder().encode(hist, (h, co) -> h.sessionOrder().in(co.ar()).not());
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void initialTxnIsFirstInArbitration() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<CeroneExecutionK, KodkodProblem> p =
        encoder()
            .encode(
                hist,
                (h, co) -> KodkodUtil.min(h.initialTransaction(), co.ar(), h.transactions()).not());
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void initialTxnPrecedesAllInSessionOrder() {
    Variable t = Variable.unary("t");

    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<CeroneExecutionK, KodkodProblem> p =
        encoder()
            .encode(
                hist,
                (h, co) ->
                    t.eq(h.initialTransaction())
                        .or(t.in(h.initialTransaction().join(h.sessionOrder())))
                        .forAll(t.oneOf(h.transactions()))
                        .not());
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void initialTxnPrecedesAllInVis() {
    Variable t = Variable.unary("t");

    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<CeroneExecutionK, KodkodProblem> p =
        encoder()
            .encode(
                hist,
                (h, co) ->
                    t.eq(h.initialTransaction())
                        .or(t.in(h.initialTransaction().join(co.vis())))
                        .forAll(t.oneOf(h.transactions()))
                        .not());
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }
}
