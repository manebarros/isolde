package haslab.isolde.experiments.benchmark;

import static haslab.isolde.IsoldeConstraint.biswas;
import static haslab.isolde.IsoldeConstraint.cerone;

import haslab.isolde.IsoldeConstraint;
import haslab.isolde.IsoldeSpec;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.biswas.definitions.TransactionalAnomalousPatterns;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.experiments.verification.FeketeReadOnlyAnomaly;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Benchmark {

  private static Map<String, IsoldeConstraint> constraintsForReadAnomaly =
      Map.of(
          "SI_c", cerone(CeroneDefinitions.SI),
          "SI_b", biswas(AxiomaticDefinitions.Snapshot),
          "UpdateSer_c", cerone(FeketeReadOnlyAnomaly::updateSer),
          "UpdateSer_b", biswas(FeketeReadOnlyAnomaly::updateSer),
          "Ser_c", cerone(CeroneDefinitions.Ser),
          "Ser_b", biswas(AxiomaticDefinitions.Ser));

  public static List<Named<IsoldeSpec>> satSameFramework() {
    List<Named<IsoldeSpec>> problems = new ArrayList<>();
    for (var level : Util.edges) {
      var pos = Util.levels.get(level.fst());
      var neg = Util.levels.get(level.snd());

      // Biswas
      String name = level.fst() + "_b\t" + level.snd() + "_b";
      problems.add(
          new Named<>(name, biswas(pos.biswasDef()).andNot(biswas(neg.biswasDef())).build()));

      // Cerone
      name = level.fst() + "_c\t" + level.snd() + "_c";
      problems.add(
          new Named<>(name, cerone(pos.ceroneDef()).andNot(cerone(neg.ceroneDef())).build()));
    }

    // Biswas read-only anomaly
    problems.add(
        new Named<>(
            "SI_b UpdateSer_b\tSer_b",
            biswas(AxiomaticDefinitions.Snapshot)
                .and(biswas(FeketeReadOnlyAnomaly::updateSer))
                .andNot(biswas(AxiomaticDefinitions.Ser))
                .build()));

    // Cerone read-only anomaly
    problems.add(
        new Named<>(
            "SI_c UpdateSer_c\tSer_c",
            cerone(CeroneDefinitions.SI)
                .and(cerone(FeketeReadOnlyAnomaly::updateSer))
                .andNot(cerone(CeroneDefinitions.Ser))
                .build()));

    problems.add(
        new Named<>(
            "PlumeRA_b\tRA_b",
            biswas(TransactionalAnomalousPatterns.ReadAtomic)
                .andNot(biswas(AxiomaticDefinitions.ReadAtomic))
                .build()));

    return problems;
  }

  public static List<Named<IsoldeSpec>> satDiffFramework() {
    List<Named<IsoldeSpec>> problems = new ArrayList<>();
    for (var level : Util.edges) {
      var pos = Util.levels.get(level.fst());
      var neg = Util.levels.get(level.snd());

      // Biswas not Cerone
      String name = level.fst() + "_b\t" + level.snd() + "_c";
      problems.add(
          new Named<>(name, biswas(pos.biswasDef()).andNot(cerone(neg.ceroneDef())).build()));

      // Cerone and not Biswas
      name = level.fst() + "_c\t" + level.snd() + "_b";
      problems.add(
          new Named<>(name, cerone(pos.ceroneDef()).andNot(biswas(neg.biswasDef())).build()));
    }

    // For the read-only anomaly
    for (var siName : Arrays.asList("SI_c", "SI_b")) {
      var si = constraintsForReadAnomaly.get(siName);
      for (var updateSerName : Arrays.asList("UpdateSer_c", "UpdateSer_b")) {
        var updateSer = constraintsForReadAnomaly.get(updateSerName);
        for (var serName : Arrays.asList("Ser_c", "Ser_b")) {
          var ser = constraintsForReadAnomaly.get(serName);
          if (lastChar(siName) != lastChar(updateSerName)
              || lastChar(updateSerName) != lastChar(serName)) {
            String name = String.format("%s %s\t%s", siName, updateSerName, serName);
            problems.add(new Named<>(name, si.and(updateSer).andNot(ser).build()));
          }
        }
      }
    }

    return problems;
  }

  private static char lastChar(String str) {
    return str.charAt(str.length() - 1);
  }

  public static List<Named<IsoldeSpec>> unsatDiffFramework() {
    List<Named<IsoldeSpec>> problems = new ArrayList<>();
    for (var level : Util.levels.entrySet()) {
      String levelName = level.getKey();
      ExecutionFormula<CeroneExecution> ceroneFormula = level.getValue().ceroneDef();
      ExecutionFormula<BiswasExecution> biswasFormula = level.getValue().biswasDef();

      // Biswas and not Cerone
      String name = levelName + "_b\t" + levelName + "_c";
      problems.add(new Named<>(name, biswas(biswasFormula).andNot(cerone(ceroneFormula)).build()));

      // Cerone and not Biswas
      name = levelName + "_c\t" + levelName + "_b";
      problems.add(new Named<>(name, cerone(ceroneFormula).andNot(biswas(biswasFormula)).build()));
    }

    return problems;
  }

  public static List<Named<IsoldeSpec>> unsatSameFramework() {
    List<Named<IsoldeSpec>> problems = new ArrayList<>();
    for (var level : Util.edges) {
      var pos = Util.levels.get(level.fst());
      var neg = Util.levels.get(level.snd());

      // Biswas
      String name = level.snd() + "_b\t" + level.fst() + "_b";
      problems.add(
          new Named<>(name, biswas(neg.biswasDef()).andNot(biswas(pos.biswasDef())).build()));

      // Cerone
      name = level.snd() + "_c\t" + level.fst() + "_c";
      problems.add(
          new Named<>(name, cerone(neg.ceroneDef()).andNot(cerone(pos.ceroneDef())).build()));
    }

    problems.add(
        new Named<>(
            "RA_b\tPlumeRA_b",
            biswas(AxiomaticDefinitions.ReadAtomic)
                .andNot(biswas(TransactionalAnomalousPatterns.ReadAtomic))
                .build()));
    problems.add(
        new Named<>(
            "CC_b\tPlumeCC_b",
            biswas(AxiomaticDefinitions.Causal)
                .andNot(biswas(TransactionalAnomalousPatterns.Causal))
                .build()));
    problems.add(
        new Named<>(
            "PlumeCC_b\tCC_b",
            biswas(TransactionalAnomalousPatterns.Causal)
                .andNot(biswas(AxiomaticDefinitions.Causal))
                .build()));

    return problems;
  }

  enum SpecClass {
    SAT_SAME_FW,
    SAT_DIFF_FW,
    UNSAT_SAME_FW,
    UNSAT_DIFF_FW
  }

  public static List<Named<IsoldeSpec>> getProblemSet(SpecClass specClass) {
    return switch (specClass) {
      case SAT_SAME_FW -> satSameFramework();
      case SAT_DIFF_FW -> satDiffFramework();
      case UNSAT_SAME_FW -> unsatSameFramework();
      case UNSAT_DIFF_FW -> unsatDiffFramework();
    };
  }

  public static Named<IsoldeSpec> getRepresentativeProblem(SpecClass specClass) {
    return switch (specClass) {
      case SAT_SAME_FW ->
          new Named<>(
              "SI_b UpdateSer_b\tSer_b",
              biswas(AxiomaticDefinitions.Snapshot)
                  .and(biswas(FeketeReadOnlyAnomaly::updateSer))
                  .andNot(biswas(AxiomaticDefinitions.Ser))
                  .build());
      case SAT_DIFF_FW ->
          new Named<>(
              "SI_b UpdateSer_c\tSer_c",
              biswas(AxiomaticDefinitions.Snapshot)
                  .and(cerone(FeketeReadOnlyAnomaly::updateSer))
                  .andNot(cerone(CeroneDefinitions.Ser))
                  .build());
      case UNSAT_SAME_FW ->
          new Named<>(
              "CC_b\tPlumeCC_b",
              biswas(AxiomaticDefinitions.Causal)
                  .andNot(biswas(TransactionalAnomalousPatterns.Causal))
                  .build());
      case UNSAT_DIFF_FW ->
          new Named<>(
              "RA_b\tRA_c",
              biswas(AxiomaticDefinitions.ReadAtomic).andNot(cerone(CeroneDefinitions.RA)).build());
    };
  }
}
