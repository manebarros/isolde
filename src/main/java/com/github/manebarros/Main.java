package com.github.manebarros;

import java.util.Arrays;
import java.util.List;
import kodkod.engine.Solution;

public class Main {
  private record Definition(
      String name,
      ExecutionFormulaK<BiswasExecutionK> biswasSpec,
      ExecutionFormulaK<CeroneExecutionK> ceroneSpec) {}

  private static final List<Definition> definitions =
      Arrays.asList(
          new Definition("Read Atomic", AxiomaticDefinitions::ReadAtomic, CeroneDefinitions.RA),
          new Definition("Causal Consistency", AxiomaticDefinitions::Causal, CeroneDefinitions.CC),
          new Definition("Prefix Consistency", AxiomaticDefinitions::Prefix, CeroneDefinitions.PC),
          new Definition(
              "Snapshot Isolation",
              (h, e) -> AxiomaticDefinitions.Prefix(h, e).and(AxiomaticDefinitions.Conflict(h, e)),
              CeroneDefinitions.SI),
          new Definition(
              "Serializability", AxiomaticDefinitions::Serializability, CeroneDefinitions.SER));

  public static void main(String[] args) {
    Scope scope = new Scope(4, 4, 4, 4);
    CegisSynthesizer synthesizer =
        new CegisSynthesizer(
            DirectSynthesisEncoder.instance(),
            CeroneCheckingEncoder.instance(),
            BiswasCheckingEncoder.instance());

    for (var def : definitions) {

      Contextualized<Solution> biswas_not_cerone_sol =
          synthesizer.synthesize(
              scope,
              new SynthesisSpec<>(def.biswasSpec()),
              SynthesisSpec.fromUniversal(def.ceroneSpec().not()));

      Contextualized<Solution> cerone_not_biswas_sol =
          synthesizer.synthesize(
              scope,
              SynthesisSpec.fromUniversal(def.biswasSpec().not()),
              new SynthesisSpec<>(def.ceroneSpec()));

      if (biswas_not_cerone_sol.getContent().unsat()
          && cerone_not_biswas_sol.getContent().unsat()) {
        System.out.println(def.name() + ": OK");
      } else {
        System.out.println(def.name() + ": not okay");
      }
    }
  }
}
