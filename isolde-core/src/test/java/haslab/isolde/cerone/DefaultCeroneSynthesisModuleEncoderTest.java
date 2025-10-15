package haslab.isolde.cerone;

import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.HistoryAtoms;
import java.util.Optional;
import kodkod.instance.TupleSet;

public class DefaultCeroneSynthesisModuleEncoderTest implements CeroneSynthesisModuleEncoderTest {

  @Override
  public ExecutionModuleConstructor<
          CeroneExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
      constructor() {
    return CeroneSynthesisModule::new;
  }
}
