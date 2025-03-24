package com.github.manebarros.cerone;

import com.github.manebarros.SynthesisModuleEncoderTest;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.SynthesisModuleEncoder;
import com.github.manebarros.kodkod.KodkodUtil;
import org.junit.jupiter.api.Test;

public interface CeroneSynthesisModuleEncoderTest
    extends SynthesisModuleEncoderTest<CeroneExecution> {

  @Override
  SynthesisModuleEncoder<CeroneExecution> encoder();

  @Test
  default void visIsInAr() {
    assertFact(e -> e.vis().in(e.ar()));
  }

  @Test
  default void visIsInArInExtraExecution() {
    assertFactWoTotalOrder(e -> e.vis().in(e.ar()));
  }

  @Test
  default void arTotallyOrdersTransactions() {
    assertFact(e -> KodkodUtil.strictTotalOrder(e.ar(), e.history().transactions()));
  }

  @Test
  default void arTotallyOrdersTransactionsInExtraExecution() {
    assertFactWoTotalOrder(e -> KodkodUtil.strictTotalOrder(e.ar(), e.history().transactions()));
  }

  @Test
  default void soInAr() {
    assertFact(e -> e.history().sessionOrder().in(e.ar()));
  }

  @Test
  default void soInArInExtraExecution() {
    assertFactWoTotalOrder(e -> e.history().sessionOrder().in(e.ar()));
  }

  @Test
  default void soMightNotBeInVis() {
    ExecutionFormula<CeroneExecution> f = e -> e.history().sessionOrder().in(e.vis()).not();
    assertSat(f);
    assertSatWoTotalOrder(f);
  }
}
