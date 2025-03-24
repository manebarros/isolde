package com.github.manebarros;

import com.github.manebarros.biswas.BiswasExecution;
import com.github.manebarros.core.SynthesisModuleEncoder;
import com.github.manebarros.kodkod.KodkodUtil;
import java.util.Arrays;
import kodkod.ast.Formula;
import kodkod.ast.Variable;
import org.junit.jupiter.api.Test;

public interface BiswasSynthesisModuleEncoderTest
    extends SynthesisModuleEncoderTest<BiswasExecution> {

  @Override
  SynthesisModuleEncoder<BiswasExecution> encoder();

  @Test
  default void commitOrderExtendsSessionOrderPlusWr() {
    assertFact(e -> e.history().sessionOrder().union(e.history().binaryWr()).in(e.co()));
  }

  @Test
  default void extraCommitOrdersExtendsSessionOrderPlusWr() {
    assertFactWoTotalOrder(
        e -> e.history().sessionOrder().union(e.history().binaryWr()).in(e.co()));
  }

  @Test
  default void commitOrderTotallyOrdersTransactions() {
    assertFact(e -> KodkodUtil.strictTotalOrder(e.co(), e.history().transactions()));
  }

  @Test
  default void initialTxnIsFirstInCommitOrder() {
    assertFact(
        e -> KodkodUtil.min(e.history().initialTransaction(), e.co(), e.history().transactions()));
  }

  @Test
  default void eachNormalTransactionBelongsToOneSession() {
    Variable t = Variable.unary("t");
    assertFact(
        e ->
            t.join(e.history().txn_session())
                .one()
                .and(t.join(e.history().txn_session()).in(e.history().sessions()))
                .forAll(t.oneOf(e.history().normalTxns())));
  }

  @Test
  default void initialTxnBelongsToNoSession() {
    assertFact(e -> e.history().initialTransaction().join(e.history().txn_session()).no());
  }

  @Test
  default void eachSessionTotallyOrdersItsTransactions() {
    Variable s = Variable.unary("s");
    assertFact(
        e ->
            KodkodUtil.strictTotalOrder(
                    e.history().sessionOrder(), e.history().txn_session().join(s))
                .forAll(s.oneOf(e.history().sessions())));
  }

  @Test
  default void sessionOrderOnlyRelatesTxnFromSameSession() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    assertFact(
        e ->
            e.history()
                .sessionOrdered(t, s)
                .implies(e.history().session(t).eq(e.history().session(s)))
                .forAll(t.oneOf(e.history().normalTxns()).and(s.oneOf(e.history().normalTxns()))));
  }

  @Test
  default void noEmptyTransactions() {
    assertFact(
        e ->
            e.history()
                .finalWrites()
                .union(e.history().externalReads())
                .join(e.history().values())
                .join(e.history().keys())
                .eq(e.history().transactions()));
  }

  @Test
  default void itIsPossibleToSynthesizeTwoCommitOrders() {
    assertSat(Arrays.asList(e -> Formula.TRUE, e -> Formula.TRUE));
  }

  @Test
  default void itIsImpossibleToSynthesizeTriviallyUnsatFormula() {
    assertUnsat(e -> Formula.FALSE);
  }
}
