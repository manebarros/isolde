package haslab.isolde;

import haslab.isolde.biswas.BiswasCandCheckingEncoder;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.BiswasSynthesisModule;
import haslab.isolde.cerone.CeroneCandCheckingModuleEncoder;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.CeroneSynthesisModule;
import haslab.isolde.core.check.candidate.CandChecker;
import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.naive.NaiveSynthesizer;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.FolSynthesisProblem;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.core.synth.Scope;
import java.util.List;
import java.util.Optional;
import kodkod.engine.config.Options;
import kodkod.instance.TupleSet;

public class NaiveIsoldeSynthesizer implements SynthesizerI {
  private final Options synthOptions;
  private final Options checkOptions;

  public NaiveIsoldeSynthesizer() {
    this(new Options());
  }

  public NaiveIsoldeSynthesizer(Options options) {
    this(options, options);
  }

  public NaiveIsoldeSynthesizer(Options synthOptions, Options checkOptions) {
    this.synthOptions = synthOptions;
    this.checkOptions = checkOptions;
  }

  public SynthesizedHistory synthesize(Scope scope, IsoldeSpec spec) {
    return synthesize(scope, spec, this.synthOptions, this.checkOptions);
  }

  public SynthesizedHistory synthesize(
      Scope scope, IsoldeSpec spec, Options synthOptions, Options checkOptions) {
    FolSynthesisInput folSynthInput =
        new FolSynthesisInput.Builder(scope).formula(spec.getHistoryFormula()).build();

    FolSynthesisProblem synthesisProblem = FolSynthesisProblem.withoutTotalOrder(folSynthInput);

    NaiveSynthesizer<FolSynthesisProblem.InputWithTotalOrder, Optional<TupleSet>> synthesizer =
        new NaiveSynthesizer<>(synthesisProblem);

    List<CeroneExecution> ceroneExecutions = null;
    List<BiswasExecution> biswasExecutions = null;

    // register Cerone module
    if (spec.usesCerone()) {
      ExecutionModuleConstructor<
              CeroneExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
          ceroneSynthModuleConstructor = CeroneSynthesisModule::new;

      CandChecker<CeroneExecution> ceroneChecker =
          new CandChecker<>(new CeroneCandCheckingModuleEncoder(1));

      ceroneExecutions =
          synthesizer.register(
              spec.getCeroneSpec().get(), ceroneSynthModuleConstructor, ceroneChecker);
    }

    // register Biswas module
    if (spec.usesBiswas()) {
      ExecutionModuleConstructor<
              BiswasExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
          biswasSynthModuleConstructor = BiswasSynthesisModule::new;

      CandChecker<BiswasExecution> biswasChecker =
          new CandChecker<>(new BiswasCandCheckingEncoder(1));

      biswasExecutions =
          synthesizer.register(
              spec.getBiswasSpec().get(), biswasSynthModuleConstructor, biswasChecker);
    }

    return new SynthesizedHistory(
        synthesizer.synthesize(synthOptions, checkOptions), ceroneExecutions, biswasExecutions);
  }
}
