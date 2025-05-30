package haslab.isolde;

import static org.junit.jupiter.api.Assertions.assertTrue;

import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.FolSynthesisProblem;
import haslab.isolde.core.synth.noSession.SimpleScope;
import haslab.isolde.history.History;
import haslab.isolde.kodkod.KodkodUtil;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.instance.TupleSet;
import org.junit.jupiter.api.Test;

public interface SimpleHistorySynthesisEncoderTest {
  HistoryEncoder<FolSynthesisInput, TupleSet> encoder();

  default SimpleScope scope() {
    return new SimpleScope(6, 6, 6);
  }

  default void assertSat(HistoryFormula formula) {
    Solution sol =
        new FolSynthesisProblem(scope(), formula, encoder()).encode().solve(new Solver());

    assertTrue(sol.sat());
  }

  default void assertUnsat(HistoryFormula formula) {
    Solution sol =
        new FolSynthesisProblem(scope(), formula, encoder()).encode().solve(new Solver());

    assertTrue(sol.unsat());
  }

  default void assertFact(HistoryFormula fact) {
    Solution sol =
        new FolSynthesisProblem(scope(), fact.not(), encoder()).encode().solve(new Solver());

    if (sol.sat()) {
      System.out.println(new History(this.encoder().encoding(), sol.instance()));
    }

    assertTrue(sol.unsat());
  }

  @Test
  default void txnWriteEachKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");

    assertFact(
        h ->
            x.join(t.join(h.finalWrites()))
                .lone()
                .forAll(t.oneOf(h.transactions()).and(x.oneOf(h.keys()))));
  }

  @Test
  default void txnReadEachKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");

    assertFact(
        h ->
            x.join(t.join(h.externalReads()))
                .lone()
                .forAll(t.oneOf(h.transactions()).and(x.oneOf(h.keys()))));
  }

  @Test
  default void sessionOrderIsStrictPartialOrder() {
    assertFact(h -> KodkodUtil.strictPartialOrder(h.sessionOrder(), h.transactions()));
  }

  @Test
  default void writesAreUnique() {
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    assertFact(
        h ->
            h.finalWrites()
                .join(n)
                .join(x)
                .lone()
                .forAll(x.oneOf(h.keys()).and(n.oneOf(h.values()))));
  }

  @Test
  default void inverseOfWrIsTotalFunction() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");

    assertFact(
        h ->
            h.wr()
                .join(t)
                .join(x)
                .one()
                .forAll(t.oneOf(h.txnThatReadAnyOf(h.keys())).and(x.oneOf(h.readSet(t)))));
  }

  @Test
  default void wrEdgeImpliesWrRelationship() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("m");
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    assertFact(
        h ->
            h.wr(t, x, s)
                .implies(
                    h.finalWrite(t, x, n).and(h.externalRead(s, x, n)).forSome(n.oneOf(h.values())))
                .forAll(
                    t.oneOf(h.transactions())
                        .and(s.oneOf(h.transactions()).and(x.oneOf(h.keys())))));
  }

  @Test
  default void soPlusWrIsAcyclic() {
    assertFact(h -> KodkodUtil.acyclic(h.sessionOrder().union(h.binaryWr())));
  }

  @Test
  default void initialTxnPrecedesAllInSessionOrder() {
    Variable t = Variable.unary("t");
    assertFact(
        h ->
            t.eq(h.initialTransaction())
                .or(t.in(h.initialTransaction().join(h.sessionOrder())))
                .forAll(t.oneOf(h.transactions())));
  }

  @Test
  default void eachNormalTransactionBelongsToOneSession() {
    Variable t = Variable.unary("t");
    assertFact(
        h ->
            t.join(h.txn_session())
                .one()
                .and(t.join(h.txn_session()).in(h.sessions()))
                .forAll(t.oneOf(h.normalTxns())));
  }

  @Test
  default void initialTxnBelongsToNoSession() {
    assertFact(h -> h.initialTransaction().join(h.txn_session()).no());
  }

  @Test
  default void eachSessionTotallyOrdersItsTransactions() {
    Variable s = Variable.unary("s");
    assertFact(
        h ->
            KodkodUtil.strictTotalOrder(h.sessionOrder(), h.txn_session().join(s))
                .forAll(s.oneOf(h.sessions())));
  }

  @Test
  default void sessionOrderOnlyRelatesTxnFromSameSession() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    assertFact(
        h ->
            h.sessionOrdered(t, s)
                .implies(h.session(t).eq(h.session(s)))
                .forAll(t.oneOf(h.normalTxns()).and(s.oneOf(h.normalTxns()))));
  }

  @Test
  default void noEmptyTransactions() {
    assertFact(
        h ->
            h.finalWrites()
                .union(h.externalReads())
                .join(h.values())
                .join(h.keys())
                .eq(h.transactions()));
  }
}
