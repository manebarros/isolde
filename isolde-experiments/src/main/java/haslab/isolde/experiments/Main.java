package haslab.isolde.experiments;

import haslab.isolde.Synthesizer;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.History;
import java.util.Arrays;
import java.util.List;
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

  private static void compareAcrossFrameworks() {
    for (var def : Main.levels) {
      System.out.print(
          compareBiswasCerone(
              new Scope(5),
              "Cerone's " + def.name(),
              def.ceroneDef(),
              "Biswas' " + def.name(),
              def.biswasDef()));
    }
  }

  private static record Definition(
      String name,
      ExecutionFormula<CeroneExecution> ceroneDef,
      ExecutionFormula<BiswasExecution> biswasDef) {}

  private static final List<Definition> levels =
      Arrays.asList(
          new Definition("RA", CeroneDefinitions.RA, AxiomaticDefinitions.ReadAtomic),
          new Definition("CC", CeroneDefinitions.CC, AxiomaticDefinitions.Causal),
          new Definition("PC", CeroneDefinitions.PC, AxiomaticDefinitions.Prefix),
          new Definition("SI", CeroneDefinitions.SI, AxiomaticDefinitions.Snapshot),
          new Definition("SER", CeroneDefinitions.SER, AxiomaticDefinitions.Ser));

  public static void main(String[] args) {
    compareAcrossFrameworks();
  }
}
