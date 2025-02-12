package com.github.manebarros;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import org.junit.jupiter.api.Test;

public interface SynthesisEncoderTest {
  SynthesisEncoder encoder();

  default Scope scope() {
    return new Scope(6, 6, 6, 6);
  }

  default void assertSat(ExecutionFormulaG formula) {
    assertSat(Collections.singletonList(formula));
  }

  default void assertSat(List<ExecutionFormulaG> formulas) {
    Solution sol =
        this.encoder()
            .encode(scope(), formulas)
            .fmap(p -> new Solver().solve(p.formula(), p.bounds()))
            .getContent();

    assertTrue(sol.sat());
  }

  default void assertUnsat(List<ExecutionFormulaG> formulas) {
    Solution sol =
        this.encoder()
            .encode(scope(), formulas)
            .fmap(p -> new Solver().solve(p.formula(), p.bounds()))
            .getContent();

    assertTrue(sol.unsat());
  }

  default void assertFact(ExecutionFormulaG fact) {
    Solution sol =
        this.encoder()
            .encode(scope(), fact.not())
            .fmap(p -> new Solver().solve(p.formula(), p.bounds()))
            .getContent();

    if (sol.sat()) {
      System.out.println(sol.instance());
    }

    assertTrue(sol.unsat());
  }

  default void assertFactOnExtraCommitOrder(ExecutionFormulaG fact) {
    Solution sol =
        this.encoder()
            .encode(scope(), Arrays.asList((h, co) -> Formula.TRUE, fact.not()))
            .fmap(p -> new Solver().solve(p.formula(), p.bounds()))
            .getContent();

    if (sol.sat()) {
      System.out.println(sol.instance());
    }

    assertTrue(sol.unsat());
  }

  @Test
  default void txnWriteEachKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");

    assertFact(
        (h, co) ->
            x.join(t.join(h.finalWrites()))
                .lone()
                .forAll(t.oneOf(h.transactions()).and(x.oneOf(h.keys()))));
  }

  @Test
  default void txnReadEachKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");

    assertFact(
        (h, co) ->
            x.join(t.join(h.externalReads()))
                .lone()
                .forAll(t.oneOf(h.transactions()).and(x.oneOf(h.keys()))));
  }

  @Test
  default void sessionOrderIsStrictPartialOrder() {
    assertFact((h, co) -> KodkodUtil.strictPartialOrder(h.sessionOrder(), h.transactions()));
  }

  @Test
  default void writesAreUnique() {
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    assertFact(
        (h, co) ->
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
        (h, co) ->
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
        (h, co) ->
            h.wr(t, x, s)
                .implies(
                    h.finalWrite(t, x, n).and(h.externalRead(s, x, n)).forSome(n.oneOf(h.values())))
                .forAll(
                    t.oneOf(h.transactions())
                        .and(s.oneOf(h.transactions()).and(x.oneOf(h.keys())))));
  }

  @Test
  default void soPlusWrIsAcyclic() {
    assertFact((h, co) -> KodkodUtil.acyclic(h.sessionOrder().union(h.binaryWr())));
  }

  @Test
  default void initialTxnPrecedesAllInSessionOrder() {
    Variable t = Variable.unary("t");
    assertFact(
        (h, co) ->
            t.eq(h.initialTransaction())
                .or(t.in(h.initialTransaction().join(h.sessionOrder())))
                .forAll(t.oneOf(h.transactions())));
  }

  @Test
  default void commitOrderExtendsSessionOrderPlusWr() {
    assertFact((h, co) -> h.sessionOrder().union(h.binaryWr()).in(co));
  }

  @Test
  default void extraCommitOrdersExtendsSessionOrderPlusWr() {
    assertFactOnExtraCommitOrder((h, co) -> h.sessionOrder().union(h.binaryWr()).in(co));
  }

  @Test
  default void commitOrderTotallyOrdersTransactions() {
    assertFact((h, co) -> KodkodUtil.strictTotalOrder(co, h.transactions()));
  }

  @Test
  default void initialTxnIsFirstInCommitOrder() {
    assertFact((h, co) -> KodkodUtil.min(h.initialTransaction(), co, h.transactions()));
  }

  @Test
  default void eachNormalTransactionBelongsToOneSession() {
    Variable t = Variable.unary("t");
    assertFact(
        (h, co) ->
            t.join(h.txn_session())
                .one()
                .and(t.join(h.txn_session()).in(h.sessions()))
                .forAll(t.oneOf(h.normalTxns())));
  }

  @Test
  default void initialTxnBelongsToNoSession() {
    assertFact((h, co) -> h.initialTransaction().join(h.txn_session()).no());
  }

  @Test
  default void eachSessionTotallyOrdersItsTransactions() {
    Variable s = Variable.unary("s");
    assertFact(
        (h, co) ->
            KodkodUtil.strictTotalOrder(h.sessionOrder(), h.txn_session().join(s))
                .forAll(s.oneOf(h.sessions())));
  }

  @Test
  default void sessionOrderOnlyRelatesTxnFromSameSession() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    assertFact(
        (h, co) ->
            h.sessionOrdered(t, s)
                .implies(h.session(t).eq(h.session(s)))
                .forAll(t.oneOf(h.normalTxns()).and(s.oneOf(h.normalTxns()))));
  }

  @Test
  default void noEmptyTransactions() {
    assertFact(
        (h, co) ->
            h.finalWrites()
                .union(h.externalReads())
                .join(h.values())
                .join(h.keys())
                .eq(h.transactions()));
  }

  @Test
  default void itIsPossibleToSynthesizeTwoCommitOrders() {
    assertSat(Arrays.asList((h, co) -> Formula.TRUE, (h, co) -> Formula.TRUE));
  }

  @Test
  default void itIsImpossibleToSynthesizeTriviallyUnsatFormula() {
    assertUnsat(Arrays.asList((h, co) -> Formula.FALSE));
  }
}
