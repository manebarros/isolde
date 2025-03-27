package com.github.manebarros;

import com.github.manebarros.biswas.BiswasExecution;
import com.github.manebarros.biswas.definitions.AxiomaticDefinitions;
import com.github.manebarros.biswas.definitions.TransactionalAnomalousPatterns;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.Scope;
import com.github.manebarros.core.SynthesisSpec;
import com.github.manebarros.history.History;
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

  private static ComparisonResult compare(
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

  public static void main(String[] args) {
    Map<String, ExecutionFormula<BiswasExecution>> definitions =
        Map.of(
            "axiomatic RA",
            AxiomaticDefinitions::ReadAtomic,
            "axiomatic CC",
            AxiomaticDefinitions::Causal,
            "TAP-based RA",
            e -> TransactionalAnomalousPatterns.l(e).not(),
            "TAP-based CC",
            e -> TransactionalAnomalousPatterns.n(e).not());

    List<String> names = new ArrayList<>(definitions.keySet());
    for (int i = 0; i < names.size(); i++) {
      for (int j = i + 1; j < names.size(); j++) {
        String a = names.get(i);
        String b = names.get(j);
        System.out.print(
            compare(new Scope(6, 6, 6, 6), a, definitions.get(a), b, definitions.get(b)));
        if (i != names.size() - 2 || j != names.size() - 1) {
          System.out.println("----------------------------------------------");
        }
      }
    }
  }
}
