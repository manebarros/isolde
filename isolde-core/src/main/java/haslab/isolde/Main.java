package haslab.isolde;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.biswas.definitions.TransactionalAnomalousPatterns;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.cerone.definitions.CustomDefinitions;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.History;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Main {
  private static record ComparisonResult(
      String a, String b, Optional<History> a_not_b, Optional<History> b_not_a) {

    @Override
    public final String toString() {
      String str;
      if (b_not_a.isPresent() && a_not_b.isPresent()) {
        // incomparable
        str =
            String.format(
                "%s and %s are INCOMPARABLE.\n"
                    + "History allowed by %s but not by %s:\n"
                    + "%s\n\n"
                    + "History allowed by %s but not by %s:\n"
                    + "%s\n"
                    + " ",
                a, b, a, b, a_not_b.get(), b, a, b_not_a.get());
      } else if (b_not_a.isPresent() && a_not_b.isEmpty()) {
        // A is stronger than B
        str =
            String.format(
                "%s is STRONGER than %s.\n" + "History allowed by %s but not by %s:\n" + "%s\n",
                a, b, b, a, b_not_a.get());
      } else if (a_not_b.isPresent() && b_not_a.isEmpty()) {
        // B is stronger than A
        str =
            String.format(
                "%s is STRONGER than %s.\n" + "History allowed by %s but not by %s:\n" + "%s\n",
                b, a, a, b, a_not_b.get());
      } else if (a_not_b.isEmpty() && b_not_a.isEmpty()) {
        // equivalent
        str = String.format("%s and %s are EQUIVALENT.\n", a, b);
      } else {
        str = String.format("what");
      }
      return str;
    }
  }

  private static ComparisonResult compareBiswasCerone(
      Scope scope,
      String a_name,
      ExecutionFormula<CeroneExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {
    Synthesizer synth = new Synthesizer(scope);
    synth.registerCerone(new SynthesisSpec<>(a_def));
    synth.registerBiswas(SynthesisSpec.fromUniversal(b_def.not()));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(b_def));
    synth.registerCerone(SynthesisSpec.fromUniversal(a_def.not()));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }

  private static record Definition(
      String name,
      ExecutionFormula<CeroneExecution> ceroneDefinition,
      ExecutionFormula<BiswasExecution> biswasDefinition) {}

  private static ComparisonResult compareBiswas(
      Scope scope,
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {
    Synthesizer synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(a_def, b_def.not()));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(b_def, a_def.not()));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }

  private static ComparisonResult compareBiswasExecution(
      Scope scope,
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {
    Synthesizer synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(a_def.and(b_def.not())));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(b_def.and(a_def.not())));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }

  public static ComparisonResult compareBiswasAndHistoryBased(
      Scope scope,
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      HistoryFormula b_def) {
    Synthesizer synth = new Synthesizer(scope, b_def.not());
    synth.registerBiswas(new SynthesisSpec<>(a_def));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope, b_def);
    synth.registerBiswas(SynthesisSpec.fromUniversal(a_def.not()));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }

  private static ComparisonResult compareCerone(
      Scope scope,
      String a_name,
      ExecutionFormula<CeroneExecution> a_def,
      String b_name,
      ExecutionFormula<CeroneExecution> b_def) {
    Synthesizer synth = new Synthesizer(scope);
    synth.registerCerone(new SynthesisSpec<>(a_def, b_def.not()));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope);
    synth.registerCerone(new SynthesisSpec<>(b_def, a_def.not()));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }

  public static void compareCeroneDefinitions() {
    Map<String, ExecutionFormula<CeroneExecution>> definitions =
        Map.of("Standard PSI", CeroneDefinitions.PSI, "Custom PSI", CustomDefinitions.customPSI);
    // Map.of("UA", CeroneDefinitions.UA, "No Lost Updates", CustomDefinitions.customUA);
    List<String> names = new ArrayList<>(definitions.keySet());
    for (int i = 0; i < names.size(); i++) {
      for (int j = i + 1; j < names.size(); j++) {
        String a = names.get(i);
        String b = names.get(j);
        System.out.print(compareCerone(new Scope(4), a, definitions.get(a), b, definitions.get(b)));
        if (i != names.size() - 2 || j != names.size() - 1) {
          System.out.println("----------------------------------------------");
        }
      }
    }
  }

  public static void verifyHistoryBasedDefinitons() {
    ExecutionFormula<BiswasExecution> biswasDef = AxiomaticDefinitions::Prefix;
    HistoryFormula historyDef = CustomDefinitions.newCustomPC();
    // System.out.print(
    //    compareBiswasAndHistoryBased(
    //        new Scope(5), "Biswas' PC", biswasDef, "History PC", historyDef));
    System.out.println(
        compareBiswasAndHistoryBased(
            new Scope(5),
            "Biswas' SI",
            AxiomaticDefinitions::Snapshot,
            "History-only SI",
            CustomDefinitions.customSI));
  }

  public static void compareBiswasDefinitions() {
    Map<String, ExecutionFormula<BiswasExecution>> definitions =
        Map.of(
            "axiomatic TCC",
            AxiomaticDefinitions::Causal,
            "TAP-based TCC",
            TransactionalAnomalousPatterns.n.not());

    List<String> names = new ArrayList<>(definitions.keySet());
    for (int i = 0; i < names.size(); i++) {
      for (int j = i + 1; j < names.size(); j++) {
        String a = names.get(i);
        String b = names.get(j);
        System.out.print(compareBiswas(new Scope(5), a, definitions.get(a), b, definitions.get(b)));
        if (i != names.size() - 2 || j != names.size() - 1) {
          System.out.println("----------------------------------------------");
        }
      }
    }
  }

  public static void compareBiswasDefinitionsExec() {
    Map<String, ExecutionFormula<BiswasExecution>> definitions =
        Map.of(
            "axiomatic RA",
            AxiomaticDefinitions::ReadAtomic,
            "TAP-based RA",
            TransactionalAnomalousPatterns.l.not());

    List<String> names = new ArrayList<>(definitions.keySet());
    for (int i = 0; i < names.size(); i++) {
      for (int j = i + 1; j < names.size(); j++) {
        String a = names.get(i);
        String b = names.get(j);
        System.out.print(
            compareBiswasExecution(
                new Scope(2, 1, 2, 1), a, definitions.get(a), b, definitions.get(b)));
        if (i != names.size() - 2 || j != names.size() - 1) {
          System.out.println("----------------------------------------------");
        }
      }
    }
  }

  public static void main(String[] args) {
    verifyHistoryBasedDefinitons();
  }
}
