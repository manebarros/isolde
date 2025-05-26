package haslab.isolde.cerone;

import haslab.isolde.core.general.ExecutionModule;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.TransactionTotalOrderInfo;

public class DefaultCeroneSynthesisModuleEncoderTest implements CeroneSynthesisModuleEncoderTest {

  @Override
  public ExecutionModule<FolSynthesisInput, TransactionTotalOrderInfo, CeroneExecution> encoder() {
    return CeroneSynthesisModule::new;
  }
}
