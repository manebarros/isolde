package com.github.manebarros;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.manebarros.core.FolSynthesisEncoder;
import com.github.manebarros.core.HistoryFormula;
import com.github.manebarros.core.HistorySynthesisEncoder;
import com.github.manebarros.core.Scope;
import com.github.manebarros.kodkod.KodkodUtil;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import org.junit.jupiter.api.Test;

public interface HistorySynthesisEncoderTest {
  HistorySynthesisEncoder encoder();

  default Scope scope() {
    return new Scope(6, 6, 6, 6);
  }

  default void assertSat(HistoryFormula formula) {
    Solution sol =
        new FolSynthesisEncoder(encoder(), scope(), formula).encode().solve(new Solver());

    assertTrue(sol.sat());
  }

  default void assertUnsat(HistoryFormula formula) {
    Solution sol =
        new FolSynthesisEncoder(encoder(), scope(), formula).encode().solve(new Solver());

    assertTrue(sol.unsat());
  }

  default void assertFact(HistoryFormula fact) {
    Solution sol =
        new FolSynthesisEncoder(encoder(), scope(), fact.not()).encode().solve(new Solver());

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
}
