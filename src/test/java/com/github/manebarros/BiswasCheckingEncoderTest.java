package com.github.manebarros;

import static com.github.manebarros.Operation.readOf;
import static com.github.manebarros.Operation.writeOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import org.junit.jupiter.api.Test;

public interface BiswasCheckingEncoderTest {
  CheckingEncoder<BiswasExecutionK> encoder();

  @Test
  default void historyIsDisallowedByReadAtomic() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0), writeOf(1, 1))),
                    new Transaction(2, Arrays.asList(readOf(1, 0), writeOf(0, 1))))));
    CheckingContextualized<BiswasExecutionK, KodkodProblem> p =
        encoder().encode(hist, AxiomaticDefinitions::ReadAtomic);
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
    CheckingContextualized<BiswasExecutionK, KodkodProblem> p =
        encoder().encode(hist, TransactionalAnomalousPatterns::l);
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void commitOrderExtendsSoPlusWr() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<BiswasExecutionK, KodkodProblem> p =
        encoder()
            .encode(
                hist, (h, co) -> h.sessionOrder().union(h.binaryWr()).in(co.commitOrder()).not());
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void commitOrderTotallyOrdersTransactions() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<BiswasExecutionK, KodkodProblem> p =
        encoder()
            .encode(
                hist,
                (h, co) -> KodkodUtil.strictTotalOrder(co.commitOrder(), h.transactions()).not());
    Solution sol = new Solver().solve(p.getContent().formula(), p.getContent().bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void initialTxnIsFirstInCommitOrder() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    CheckingContextualized<BiswasExecutionK, KodkodProblem> p =
        encoder()
            .encode(
                hist,
                (h, co) ->
                    KodkodUtil.min(h.initialTransaction(), co.commitOrder(), h.transactions())
                        .not());
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
    CheckingContextualized<BiswasExecutionK, KodkodProblem> p =
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
}
