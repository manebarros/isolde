package haslab.isolde.biswas;

import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.HistoryAtoms;
import java.util.Optional;
import kodkod.instance.TupleSet;

public class DefaultBiswasSynthesisModuleEncoderTest implements BiswasSynthesisModuleEncoderTest {

  @Override
  public ExecutionModuleConstructor<
          BiswasExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
      constructor() {
    return BiswasSynthesisModule::new;
  }
}
