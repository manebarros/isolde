package haslab.isolde;

import haslab.isolde.biswas.BiswasCandCheckingEncoder;
import haslab.isolde.biswas.BiswasCounterexampleEncoder;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.BiswasSynthesisModule;
import haslab.isolde.cerone.CeroneCandCheckingModuleEncoder;
import haslab.isolde.cerone.CeroneCounterexampleEncoder;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.CeroneSynthesisModule;
import haslab.isolde.core.Execution;
import haslab.isolde.core.HistoryDecls;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.CegisSynthesizer;
import haslab.isolde.core.cegis.CegisSynthesizer.CegisModule;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.check.candidate.CandCheckEncoder;
import haslab.isolde.core.check.candidate.ContextualizedInstance;
import haslab.isolde.core.check.candidate.DefaultCandCheckingEncoder;
import haslab.isolde.core.general.simple.ExecutionConstraintsEncoderS;
import haslab.isolde.core.synth.FolSynthesisProblem;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.core.synth.TransactionTotalOrderInfo;
import haslab.isolde.core.synth.noSession.SimpleScope;
import haslab.isolde.history.History;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kodkod.engine.Solution;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

public class Synthesizer {
  private CegisSynthesizer<TupleSet, TransactionTotalOrderInfo> cegisSynthesizer;
  private CegisModule<CeroneExecution> ceroneExecutions;
  private CegisModule<BiswasExecution> biswasExecutions;

  public Synthesizer(Scope scope) {
    this.cegisSynthesizer = new CegisSynthesizer<>(new FolSynthesisProblem(scope));
    this.ceroneExecutions = null;
    this.ceroneExecutions = null;
  }

  public Synthesizer(SimpleScope scope) {
    this.cegisSynthesizer = new CegisSynthesizer<>(new FolSynthesisProblem(scope));
    this.ceroneExecutions = null;
    this.ceroneExecutions = null;
  }

  public Synthesizer(Scope scope, HistoryFormula hf) {
    this.cegisSynthesizer = new CegisSynthesizer<>(new FolSynthesisProblem(scope, hf));
    this.ceroneExecutions = null;
    this.ceroneExecutions = null;
  }

  public Synthesizer(Scope scope, HistoryFormula hf, HistoryDecls decls) {
    this.cegisSynthesizer = new CegisSynthesizer<>(new FolSynthesisProblem(scope, hf, decls));
    this.ceroneExecutions = null;
    this.ceroneExecutions = null;
  }

  public static <E extends Execution> CandCheckEncoder<E> candCheckEncoder(
      ExecutionConstraintsEncoderS<ContextualizedInstance, E> moduleEncoder) {
    return new CandCheckEncoder<>(DefaultCandCheckingEncoder.instance(), moduleEncoder);
  }

  public void registerCerone(SynthesisSpec<CeroneExecution> spec) {
    this.ceroneExecutions =
        this.cegisSynthesizer.add(
            spec,
            CeroneSynthesisModule::new,
            candCheckEncoder(new CeroneCandCheckingModuleEncoder(1)),
            new CeroneCounterexampleEncoder());
  }

  public void registerBiswas(SynthesisSpec<BiswasExecution> spec) {
    this.biswasExecutions =
        this.cegisSynthesizer.add(
            spec,
            BiswasSynthesisModule::new,
            candCheckEncoder(new BiswasCandCheckingEncoder(1)),
            new BiswasCounterexampleEncoder());
  }

  public record CegisHistory(Optional<History> history, int candidates) {}

  public CegisHistory synthesizeWithInfo(SATFactory solver) {
    List<Solution> solutions = this.cegisSynthesizer.synthesize(solver);
    boolean success = solutions.getFirst().sat();
    if (success) {
      Instance instance = solutions.getFirst().instance();
      return new CegisHistory(
          Optional.of(new History(this.cegisSynthesizer.historyEncoding(), instance)),
          solutions.size());
    }
    return new CegisHistory(Optional.empty(), solutions.size() - 1);
  }

  public SynthesizedHistory synthesizeClean() {
    return synthesizeClean(SATFactory.MiniSat);
  }

  public SynthesizedHistory synthesizeClean(SATFactory solver) {
    List<Solution> candidates = this.cegisSynthesizer.synthesize(solver);
    return new SynthesizedHistory(
        candidates,
        this.cegisSynthesizer.historyEncoding(),
        this.ceroneExecutions != null
            ? this.ceroneExecutions.synthesisExecutions()
            : new ArrayList<>(),
        this.biswasExecutions != null
            ? this.biswasExecutions.synthesisExecutions()
            : new ArrayList<>());
  }

  public Optional<History> synthesize() {
    return this.cegisSynthesizer.synthesizeH();
  }

  public CegisSynthesizer<TupleSet, TransactionTotalOrderInfo> getCegisSynthesizer() {
    return cegisSynthesizer;
  }

  public CegisModule<CeroneExecution> getCeroneExecutions() {
    return ceroneExecutions;
  }

  public CegisModule<BiswasExecution> getBiswasExecutions() {
    return biswasExecutions;
  }
}
