package haslab.isolde.experiments.verification;

import static haslab.isolde.cerone.definitions.CeroneDefinitions.SER;

import haslab.isolde.SynthesizedHistory;
import haslab.isolde.Synthesizer;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryDecls;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import java.util.Arrays;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public class Scenarios {
  private Scenarios() {}

  public static record Scenario(
      Scope scope,
      HistoryFormula hf,
      HistoryDecls decls,
      SynthesisSpec<CeroneExecution> ceroneSpec,
      SynthesisSpec<BiswasExecution> biswasSpec) {

    public Scenario(
        Scope scope,
        SynthesisSpec<CeroneExecution> ceroneSpec,
        SynthesisSpec<BiswasExecution> biswasSpec) {
      this(scope, h -> Formula.TRUE, null, ceroneSpec, biswasSpec);
    }

    public static Scenario justBiswas(Scope scope, SynthesisSpec<BiswasExecution> biswasSpec) {
      return new Scenario(scope, null, biswasSpec);
    }

    public static Scenario justCerone(Scope scope, SynthesisSpec<CeroneExecution> ceroneSpec) {
      return new Scenario(scope, ceroneSpec, null);
    }

    public static Scenario justCerone(
        Scope scope,
        HistoryFormula hf,
        HistoryDecls decls,
        SynthesisSpec<CeroneExecution> ceroneSpec) {
      return new Scenario(scope, hf, decls, ceroneSpec, null);
    }
  }

  public static final Scenario simpleCausalityViolationCeroneBiswas =
      new Scenario(
          new Scope(3, 2, 2, 2),
          new SynthesisSpec<>(CeroneDefinitions.RA),
          SynthesisSpec.not(AxiomaticDefinitions.Causal));

  public static final Scenario greenRedItems() {
    Variable greenKeys = Variable.unary("green keys");
    Variable redKeys = Variable.unary("red keys");

    ExecutionFormula<CeroneExecution> serAtGreen =
        e ->
            SER.resolve(
                new CeroneExecution(e.history().projectionOverKeys(greenKeys), e.vis(), e.ar()));
    ExecutionFormula<CeroneExecution> serAtRed =
        e ->
            SER.resolve(
                new CeroneExecution(e.history().projectionOverKeys(redKeys), e.vis(), e.ar()));
    HistoryFormula hf =
        h -> greenKeys.intersection(redKeys).no().and(h.keys().eq(greenKeys.union(redKeys)));
    HistoryDecls decls = h -> greenKeys.someOf(h.keys()).and(redKeys.someOf(h.keys()));

    return Scenario.justCerone(
        new Scope(2, 2, 2, 2),
        hf,
        decls,
        new SynthesisSpec<>(Arrays.asList(serAtRed, serAtGreen), CeroneDefinitions.SER.not()));
  }

  public static SynthesizedHistory runScenario(Scenario scenario) {
    Synthesizer synth;
    if (scenario.decls() == null) {
      synth = new Synthesizer(scenario.scope(), scenario.hf());
    } else {
      synth = new Synthesizer(scenario.scope(), scenario.hf(), scenario.decls());
    }

    if (scenario.ceroneSpec() != null) {
      synth.registerCerone(scenario.ceroneSpec());
    }
    if (scenario.biswasSpec() != null) {
      synth.registerBiswas(scenario.biswasSpec());
    }
    return synth.synthesizeClean();
  }
}
