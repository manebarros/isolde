package haslab.isolde.cerone;

import haslab.isolde.SynthesisModuleEncoderTest;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.kodkod.KodkodUtil;
import java.util.Optional;
import kodkod.instance.TupleSet;
import org.junit.jupiter.api.Test;

public interface CeroneSynthesisModuleEncoderTest
    extends SynthesisModuleEncoderTest<CeroneExecution> {

  @Override
  ExecutionModuleConstructor<
          CeroneExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
      constructor();

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
