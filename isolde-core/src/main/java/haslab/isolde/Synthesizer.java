package haslab.isolde;

import haslab.isolde.biswas.BiswasCheckingEncoder;
import haslab.isolde.biswas.BiswasCounterexampleEncoder;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.BiswasSynthesisModule;
import haslab.isolde.cerone.CeroneCounterexampleEncoder;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.CeroneSynthesisModule;
import haslab.isolde.cerone.check.CeroneCheckingModuleEncoder;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.CegisSynthesizer;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.History;
import java.util.List;
import java.util.Optional;

public class Synthesizer {
  private CegisSynthesizer cegisSynthesizer;
  private List<CeroneExecution> ceroneExecutions;
  private List<BiswasExecution> biswasExecutions;

  public Synthesizer(Scope scope) {
    this.cegisSynthesizer = new CegisSynthesizer(scope);
    this.ceroneExecutions = null;
    this.ceroneExecutions = null;
  }

  public Synthesizer(Scope scope, HistoryFormula hf) {
    this.cegisSynthesizer = new CegisSynthesizer(scope, hf);
    this.ceroneExecutions = null;
    this.ceroneExecutions = null;
  }

  public void registerCerone(SynthesisSpec<CeroneExecution> spec) {
    this.ceroneExecutions =
        this.cegisSynthesizer
            .add(
                spec,
                CeroneSynthesisModule::new,
                CeroneCheckingModuleEncoder::new,
                new CeroneCounterexampleEncoder())
            .synthesisExecutions();
  }

  public void registerBiswas(SynthesisSpec<BiswasExecution> spec) {
    this.biswasExecutions =
        this.cegisSynthesizer
            .add(
                spec,
                BiswasSynthesisModule::new,
                BiswasCheckingEncoder::new,
                new BiswasCounterexampleEncoder())
            .synthesisExecutions();
  }

  public Optional<History> synthesize() {
    return this.cegisSynthesizer.synthesizeH();
  }

  public CegisSynthesizer getCegisSynthesizer() {
    return cegisSynthesizer;
  }

  public List<CeroneExecution> getCeroneExecutions() {
    return ceroneExecutions;
  }

  public List<BiswasExecution> getBiswasExecutions() {
    return biswasExecutions;
  }
}
