package haslab.isolde;

import haslab.isolde.biswas.BiswasCandCheckingEncoder;
import haslab.isolde.biswas.BiswasCounterexampleEncoder;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.BiswasSynthesisModule;
import haslab.isolde.cerone.CeroneCandCheckingModuleEncoder;
import haslab.isolde.cerone.CeroneCounterexampleEncoder;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.CeroneSynthesisModule;
import haslab.isolde.core.cegis.CegisSynthesizer;
import haslab.isolde.core.check.candidate.CandChecker;
import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.FolSynthesisProblem;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.core.synth.Scope;
import java.util.List;
import java.util.Optional;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.TupleSet;

public class IsoldeSynthesizer {
  private final Options synthOptions;
  private final Options checkOptions;
  private final boolean useTxnTotalOrder;

  private IsoldeSynthesizer(Builder builder) {
    this.synthOptions = builder.synthOptions;
    this.checkOptions = builder.checkOptions;
    this.useTxnTotalOrder = builder.useTxnTotalOrder;
  }

  public static class Builder {
    private Options synthOptions;
    private Options checkOptions;
    private boolean useTxnTotalOrder;

    public Builder() {
      this.synthOptions = new Options();
      this.checkOptions = new Options();
      this.useTxnTotalOrder = true;
    }

    public Builder synthOptions(Options options) {
      this.synthOptions = options;
      return this;
    }

    public Builder checkOptions(Options options) {
      this.checkOptions = options;
      return this;
    }

    public Builder useTxnTotalOrder(boolean val) {
      this.useTxnTotalOrder = val;
      return this;
    }

    public Builder synthSolver(SATFactory solver) {
      this.synthOptions.setSolver(solver);
      return this;
    }

    public Builder checkSolver(SATFactory solver) {
      this.checkOptions.setSolver(solver);
      return this;
    }

    public IsoldeSynthesizer build() {
      return new IsoldeSynthesizer(this);
    }
  }

  public SynthesizedHistory synthesize(Scope scope, IsoldeConstraint spec) {
    return synthesize(scope, new IsoldeSpec.Builder(spec).build());
  }

  public SynthesizedHistory synthesize(Scope scope, IsoldeSpec spec) {
    FolSynthesisInput folSynthInput =
        new FolSynthesisInput.Builder(scope).formula(spec.getHistoryFormula()).build();

    FolSynthesisProblem synthesisProblem =
        useTxnTotalOrder
            ? FolSynthesisProblem.withTotalOrder(folSynthInput)
            : FolSynthesisProblem.withNoTotalOrder(folSynthInput);

    var synthesizer = new CegisSynthesizer<>(synthesisProblem);

    // register Cerone module
    ExecutionModuleConstructor<
            CeroneExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
        ceroneSynthModuleConstructor = CeroneSynthesisModule::new;

    CandChecker<CeroneExecution> ceroneChecker =
        new CandChecker<>(new CeroneCandCheckingModuleEncoder(1));

    List<CeroneExecution> ceroneExecutions =
        synthesizer.register(
            spec.getCeroneSpec(),
            ceroneSynthModuleConstructor,
            ceroneChecker,
            CeroneCounterexampleEncoder.instance());

    // register Biswas module
    ExecutionModuleConstructor<
            BiswasExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
        biswasSynthModuleConstructor = BiswasSynthesisModule::new;

    CandChecker<BiswasExecution> biswasChecker =
        new CandChecker<>(new BiswasCandCheckingEncoder(1));

    List<BiswasExecution> biswasExecutions =
        synthesizer.register(
            spec.getBiswasSpec(),
            biswasSynthModuleConstructor,
            biswasChecker,
            BiswasCounterexampleEncoder.instance());

    return new SynthesizedHistory(
        synthesizer.synthesize(synthOptions, checkOptions), ceroneExecutions, biswasExecutions);
  }
}
