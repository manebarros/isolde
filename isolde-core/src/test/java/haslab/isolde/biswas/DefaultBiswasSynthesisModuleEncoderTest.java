package haslab.isolde.biswas;

import haslab.isolde.core.general.ExecutionModule;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.TransactionTotalOrderInfo;

public class DefaultBiswasSynthesisModuleEncoderTest implements BiswasSynthesisModuleEncoderTest {

  @Override
  public ExecutionModule<FolSynthesisInput, TransactionTotalOrderInfo, BiswasExecution> encoder() {
    return BiswasSynthesisModule::new;
  }
}
