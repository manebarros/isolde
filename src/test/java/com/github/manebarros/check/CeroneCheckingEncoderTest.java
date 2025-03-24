package com.github.manebarros.check;

import static com.github.manebarros.history.Operation.readOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.github.manebarros.cerone.CeroneExecution;
import com.github.manebarros.core.CheckingEncoder;
import com.github.manebarros.history.History;
import com.github.manebarros.history.Session;
import com.github.manebarros.history.Transaction;
import com.github.manebarros.kodkod.KodkodProblem;
import com.github.manebarros.kodkod.KodkodUtil;

import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;

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
  CheckingEncoder<CeroneExecution> encoder();

  @Test
  default void arbitrationTotallyOrdersTransactions() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    KodkodProblem p = 
        encoder()
            .encode(hist, e -> KodkodUtil.strictTotalOrder(e.ar(), e.history().transactions()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p =
        encoder().encode(hist, e -> e.vis().in(e.ar()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p = 
        encoder().encode(hist, e -> e.history().sessionOrder().in(e.vis()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p = 
        encoder().encode(hist, e -> e.history().sessionOrder().in(e.ar()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p = 
        encoder()
            .encode(
                hist,
                e -> KodkodUtil.min(e.history().initialTransaction(), e.ar(), e.history().transactions()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p = 
        encoder()
            .encode(
                hist,
                e ->
                    t.eq(e.history().initialTransaction())
                        .or(t.in(e.history().initialTransaction().join(e.history().sessionOrder())))
                        .forAll(t.oneOf(e.history().transactions()))
                        .not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p = 
        encoder()
            .encode(
                hist,
                e ->
                    t.eq(e.history().initialTransaction())
                        .or(t.in(e.history().initialTransaction().join(e.vis())))
                        .forAll(t.oneOf(e.history().transactions()))
                        .not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }
}
