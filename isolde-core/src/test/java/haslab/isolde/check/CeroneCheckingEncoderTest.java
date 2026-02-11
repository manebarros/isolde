package haslab.isolde.check;

import static haslab.isolde.history.Operation.readOf;
import static haslab.isolde.history.Operation.writeOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.CeroneHistCheckingModuleEncoder;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.check.external.CheckingIntermediateRepresentation;
import haslab.isolde.core.check.external.HistCheckEncoder;
import haslab.isolde.core.general.DirectExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.history.History;
import haslab.isolde.history.Session;
import haslab.isolde.history.Transaction;
import haslab.isolde.kodkod.KodkodUtil;
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

  CeroneHistCheckingModuleEncoder histCheckModuleEncoder(int executions);

  default HistCheckEncoder<CeroneExecution> histCheckEncoder() {
    DirectExecutionModuleConstructor<
            CeroneExecution,
            CheckingIntermediateRepresentation,
            SimpleContext<CheckingIntermediateRepresentation>>
        constructor = this::histCheckModuleEncoder;
    return new HistCheckEncoder<CeroneExecution>(constructor);
  }

  @Test
  default void arbitrationTotallyOrdersTransactions() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    Solution sol =
        histCheckEncoder()
            .check(
                hist,
                e -> KodkodUtil.strictTotalOrder(e.ar(), e.history().transactions()).not(),
                new Solver());
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
    Solution sol = histCheckEncoder().check(hist, e -> e.vis().in(e.ar()).not(), new Solver());
    assertTrue(sol.unsat());
  }

  @Test
  default void soSubsetOfVis() {
    History hist =
        new History(
            new Session(
                Arrays.asList(
                    new Transaction(1, Arrays.asList(readOf(0, 0))),
                    new Transaction(2, Arrays.asList(readOf(0, 0))),
                    new Transaction(3, Arrays.asList(readOf(0, 0))))));
    Solution sol =
        histCheckEncoder()
            .check(hist, e -> e.history().sessionOrder().in(e.vis()).not(), new Solver());
    assertTrue(sol.unsat());
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
    Solution sol =
        histCheckEncoder()
            .check(hist, e -> e.history().sessionOrder().in(e.ar()).not(), new Solver());
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
    Solution sol =
        histCheckEncoder()
            .check(
                hist,
                e ->
                    KodkodUtil.min(
                            e.history().initialTransaction(), e.ar(), e.history().transactions())
                        .not(),
                new Solver());
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
    Solution sol =
        histCheckEncoder()
            .check(
                hist,
                e ->
                    t.eq(e.history().initialTransaction())
                        .or(t.in(e.history().initialTransaction().join(e.history().sessionOrder())))
                        .forAll(t.oneOf(e.history().transactions()))
                        .not(),
                new Solver());
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
    Solution sol =
        histCheckEncoder()
            .check(
                hist,
                e ->
                    t.eq(e.history().initialTransaction())
                        .or(t.in(e.history().initialTransaction().join(e.vis())))
                        .forAll(t.oneOf(e.history().transactions()))
                        .not(),
                new Solver());
    assertTrue(sol.unsat());
  }

  @Test
  default void causalityViolationIsDisallowedByCC() {
    History hist =
        new History(
            new Session(new Transaction(1, Arrays.asList(writeOf(0, 1), writeOf(1, 1)))),
            new Session(new Transaction(2, Arrays.asList(readOf(1, 1), writeOf(1, 2)))),
            new Session(new Transaction(3, Arrays.asList(readOf(1, 2), readOf(0, 0)))));
    Solution sol = histCheckEncoder().check(hist, CeroneDefinitions.CC, new Solver());
    assertTrue(sol.unsat());
  }

  @Test
  default void causalityViolationIsAllowedByRA() {
    History hist =
        new History(
            new Session(new Transaction(1, Arrays.asList(writeOf(0, 1), writeOf(1, 1)))),
            new Session(new Transaction(2, Arrays.asList(readOf(1, 1), writeOf(1, 2)))),
            new Session(new Transaction(3, Arrays.asList(readOf(1, 2), readOf(0, 0)))));
    Solution sol = histCheckEncoder().check(hist, CeroneDefinitions.RA, new Solver());
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
    Solution sol = histCheckEncoder().check(hist, CeroneDefinitions.RA, new Solver());
    assertTrue(sol.unsat());
  }
}
