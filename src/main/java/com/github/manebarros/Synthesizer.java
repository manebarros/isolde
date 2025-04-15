package com.github.manebarros;

import com.github.manebarros.biswas.BiswasCheckingEncoder;
import com.github.manebarros.biswas.BiswasCounterexampleEncoder;
import com.github.manebarros.biswas.BiswasExecution;
import com.github.manebarros.biswas.BiswasSynthesisModule;
import com.github.manebarros.cerone.CeroneCheckingEncoder;
import com.github.manebarros.cerone.CeroneCounterexampleEncoder;
import com.github.manebarros.cerone.CeroneExecution;
import com.github.manebarros.cerone.CeroneSynthesisModule;
import com.github.manebarros.core.cegis.CegisSynthesizer;
import com.github.manebarros.core.HistoryFormula;
import com.github.manebarros.core.synth.Scope;
import com.github.manebarros.core.cegis.SynthesisSpec;
import com.github.manebarros.history.History;
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
                new CeroneCheckingEncoder(),
                new CeroneCounterexampleEncoder())
            .synthesisExecutions();
  }

  public void registerBiswas(SynthesisSpec<BiswasExecution> spec) {
    this.biswasExecutions =
        this.cegisSynthesizer
            .add(
                spec,
                BiswasSynthesisModule::new,
                new BiswasCheckingEncoder(),
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
