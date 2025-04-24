package com.github.manebarros.biswas;

import com.github.manebarros.SynthesisModuleEncoderTest;
import com.github.manebarros.core.synth.SynthesisModuleEncoder;
import com.github.manebarros.kodkod.KodkodUtil;
import java.util.Arrays;
import kodkod.ast.Formula;
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
  default void itIsPossibleToSynthesizeTwoCommitOrders() {
    assertSat(Arrays.asList(e -> Formula.TRUE, e -> Formula.TRUE));
  }
}
