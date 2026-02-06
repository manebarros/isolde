package haslab.isolde;

import haslab.isolde.biswas.BiswasCandCheckingEncoder;
import haslab.isolde.biswas.BiswasCounterexampleEncoder;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.BiswasSynthesisModule;
import haslab.isolde.cerone.CeroneCandCheckingModuleEncoder;
import haslab.isolde.cerone.CeroneCounterexampleEncoder;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.CeroneSynthesisModule;
import haslab.isolde.core.HistoryDecls;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.CegisResult;
import haslab.isolde.core.cegis.CegisSynthesizer;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.check.candidate.CandChecker;
import haslab.isolde.core.general.ExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.FolSynthesisProblem;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.History;
import java.util.List;
import java.util.Optional;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.TupleSet;

public class Synthesizer {
  private CegisSynthesizer<FolSynthesisProblem.InputWithTotalOrder, Optional<TupleSet>>
      cegisSynthesizer;
  private List<CeroneExecution> ceroneExecutions;
  private List<BiswasExecution> biswasExecutions;

  public static Synthesizer withNoTotalOrder(Scope scope) {
    var cegisSynthesizer =
        CegisSynthesizer.withSmartSearchFormula(FolSynthesisProblem.withoutTotalOrder(scope), true);
    return new Synthesizer(cegisSynthesizer);
  }

  public Synthesizer(
      CegisSynthesizer<FolSynthesisProblem.InputWithTotalOrder, Optional<TupleSet>>
          cegisSynthesizer) {
    this.cegisSynthesizer = cegisSynthesizer;
    this.ceroneExecutions = null;
    this.biswasExecutions = null;
  }

  public Synthesizer(Scope scope) {
    this.cegisSynthesizer =
        CegisSynthesizer.withSmartSearchFormula(FolSynthesisProblem.withTotalOrder(scope), true);
    this.ceroneExecutions = null;
    this.biswasExecutions = null;
  }

  public Synthesizer(Scope scope, HistoryFormula hf) {
    FolSynthesisInput input = new FolSynthesisInput.Builder(scope).formula(hf).build();
    this.cegisSynthesizer =
        CegisSynthesizer.withSmartSearchFormula(FolSynthesisProblem.withTotalOrder(input), true);
    this.ceroneExecutions = null;
    this.biswasExecutions = null;
  }

  public Synthesizer(Scope scope, HistoryFormula hf, HistoryDecls decls) {
    FolSynthesisInput input = new FolSynthesisInput.Builder(scope).formula(hf).delcs(decls).build();
    this.cegisSynthesizer =
        CegisSynthesizer.withSmartSearchFormula(FolSynthesisProblem.withTotalOrder(input), true);
    this.ceroneExecutions = null;
    this.biswasExecutions = null;
  }

  public void registerCerone(SynthesisSpec<CeroneExecution> spec) {
    ExecutionModuleConstructor<
            CeroneExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
        ceroneSynthModuleConstructor = CeroneSynthesisModule::new;

    this.ceroneExecutions =
        this.cegisSynthesizer.register(
            spec,
            ceroneSynthModuleConstructor,
            new CandChecker<>(new CeroneCandCheckingModuleEncoder(1)),
            CeroneCounterexampleEncoder.instance());
  }

  public void registerBiswas(SynthesisSpec<BiswasExecution> spec) {
    ExecutionModuleConstructor<
            BiswasExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>>
        biswasSynthModuleConstructor = BiswasSynthesisModule::new;

    this.biswasExecutions =
        this.cegisSynthesizer.register(
            spec,
            biswasSynthModuleConstructor,
            new CandChecker<>(new BiswasCandCheckingEncoder(1)),
            BiswasCounterexampleEncoder.instance());
  }

  public SynthesizedHistory synthesize(SATFactory solver) {
    CegisResult sol = this.cegisSynthesizer.synthesize(solver);
    return new SynthesizedHistory(sol, ceroneExecutions, biswasExecutions);
  }

  public Optional<CegisSynthesizer.Status<?>> debug(SATFactory solver, History history) {
    Options options = new Options();
    options.setSolver(solver);
    return this.cegisSynthesizer.identify(options, options, history);
  }

  public SynthesizedHistory synthesize() {
    return synthesize(SATFactory.MiniSat);
  }

  public List<CeroneExecution> getCeroneExecutions() {
    return ceroneExecutions;
  }

  public List<BiswasExecution> getBiswasExecutions() {
    return biswasExecutions;
  }

  public CegisSynthesizer<FolSynthesisProblem.InputWithTotalOrder, Optional<TupleSet>>
      getCegisSynthesizer() {
    return cegisSynthesizer;
  }
}
