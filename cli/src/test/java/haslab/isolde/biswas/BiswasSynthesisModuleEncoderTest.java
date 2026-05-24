package haslab.isolde.biswas;

import haslab.isolde.SynthesisModuleEncoderTest;
import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.kodkod.KodkodUtil;
import java.util.Arrays;
import java.util.Optional;
import kodkod.ast.Formula;
import kodkod.instance.TupleSet;
import org.junit.jupiter.api.Test;

public interface BiswasSynthesisModuleEncoderTest
    extends SynthesisModuleEncoderTest<BiswasExecution> {

  @Override
  ExecutionModuleConstructor<
          BiswasExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
      constructor();

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
