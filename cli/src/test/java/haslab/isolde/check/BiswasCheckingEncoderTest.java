package haslab.isolde.check;

import static haslab.isolde.history.Operation.readOf;
import static haslab.isolde.history.Operation.writeOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.biswas.definitions.TransactionalAnomalousPatterns;
import haslab.isolde.core.check.external.HistCheckEncoder;
import haslab.isolde.history.History;
import haslab.isolde.history.Session;
import haslab.isolde.history.Transaction;
import haslab.isolde.kodkod.KodkodProblem;
import haslab.isolde.kodkod.KodkodUtil;
import java.util.Arrays;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import org.junit.jupiter.api.Test;

public interface BiswasCheckingEncoderTest {
  HistCheckEncoder<BiswasExecution> encoder();

  @Test
  default void historyIsDisallowedByReadAtomic() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0), writeOf(1, 1))),
                    new Transaction(2, Arrays.asList(readOf(1, 0), writeOf(0, 1))))));
    KodkodProblem p = encoder().encode(hist, AxiomaticDefinitions::ReadAtomic);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p = encoder().encode(hist, TransactionalAnomalousPatterns::l);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p =
        encoder()
            .encode(
                hist,
                e -> e.history().sessionOrder().union(e.history().binaryWr()).in(e.co()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p =
        encoder()
            .encode(
                hist, e -> KodkodUtil.strictTotalOrder(e.co(), e.history().transactions()).not());
    Solution sol = new Solver().solve(p.formula(), p.bounds());
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
    KodkodProblem p =
        encoder()
            .encode(
                hist,
                e ->
                    KodkodUtil.min(
                            e.history().initialTransaction(), e.co(), e.history().transactions())
                        .not());
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
  default void causalityViolationIsDisallowedByCC() {
    History hist =
        new History(
            new Session(new Transaction(1, Arrays.asList(writeOf(0, 1), writeOf(1, 1)))),
            new Session(new Transaction(2, Arrays.asList(readOf(1, 1), writeOf(1, 2)))),
            new Session(new Transaction(3, Arrays.asList(readOf(1, 2), readOf(0, 0)))));
    KodkodProblem p = encoder().encode(hist, AxiomaticDefinitions::Causal);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }

  @Test
  default void causalityViolationIsAllowedByRA() {
    History hist =
        new History(
            new Session(new Transaction(1, Arrays.asList(writeOf(0, 1), writeOf(1, 1)))),
            new Session(new Transaction(2, Arrays.asList(readOf(1, 1), writeOf(1, 2)))),
            new Session(new Transaction(3, Arrays.asList(readOf(1, 2), readOf(0, 0)))));
    KodkodProblem p = encoder().encode(hist, AxiomaticDefinitions::ReadAtomic);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.sat());
  }

  @Test
  default void txnMustBeAwareOfPrecedingTxnInSesssionOrderInRA() {
    History hist =
        new History(
            new Session(
                new Transaction(1, Arrays.asList(readOf(1, 0), writeOf(0, 2), writeOf(1, 1))),
                new Transaction(2, Arrays.asList(readOf(1, 0), writeOf(0, 2), writeOf(1, 1)))),
            new Session(
                new Transaction(3, Arrays.asList(readOf(1, 0), writeOf(0, 2), writeOf(1, 1)))));
    KodkodProblem p = encoder().encode(hist, AxiomaticDefinitions::ReadAtomic);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    assertTrue(sol.unsat());
  }
}
