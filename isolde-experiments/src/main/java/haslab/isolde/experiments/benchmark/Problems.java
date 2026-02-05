package haslab.isolde.experiments.benchmark;

import static haslab.isolde.IsoldeConstraint.biswas;
import static haslab.isolde.IsoldeConstraint.cerone;

import haslab.isolde.IsoldeConstraint;
import haslab.isolde.IsoldeSpec;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.biswas.definitions.TransactionalAnomalousPatterns;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.experiments.verification.FeketeReadOnlyAnomaly;
import haslab.isolde.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Problems {

  public static enum Framework {
    CER,
    BIS;

    public static Framework fromString(String str) {
      return str.equals("c") ? CER : BIS;
    }
  }

  public static record DefinitionId(String levelName, String style, Framework framework) {
    public static DefinitionId fromString(String str) {
      String[] parts = str.split("_");
      assert parts.length == 2 || parts.length == 3;
      if (parts.length == 2 || parts[1].isEmpty()) {
        return new DefinitionId(parts[0], Framework.fromString(parts[1]));
      }
      return new DefinitionId(parts[0], parts[1], Framework.fromString(parts[2]));
    }

    public static DefinitionId bis(String levelName) {
      return new DefinitionId(levelName, Framework.BIS);
    }

    public static DefinitionId cer(String levelName) {
      return new DefinitionId(levelName, Framework.CER);
    }

    public static DefinitionId bis(String levelName, String style) {
      return new DefinitionId(levelName, style, Framework.BIS);
    }

    public static DefinitionId cer(String levelName, String style) {
      return new DefinitionId(levelName, style, Framework.CER);
    }

    public DefinitionId(String levelName, Framework framework) {
      this(levelName, "", framework);
    }

    public String toString() {
      return levelName + "_" + style + "_" + (this.framework == Framework.CER ? "c" : "b");
    }
  }

  private static Map<DefinitionId, IsoldeConstraint> definitions = buildDefinitions();

  private static Map<DefinitionId, IsoldeConstraint> buildDefinitions() {
    Map<DefinitionId, IsoldeConstraint> m = new HashMap<>();
    // biswas axiomatic
    m.put(new DefinitionId("RA", "ax", Framework.BIS), biswas(AxiomaticDefinitions.ReadAtomic));
    m.put(new DefinitionId("CC", "ax", Framework.BIS), biswas(AxiomaticDefinitions.Causal));
    m.put(new DefinitionId("PC", "ax", Framework.BIS), biswas(AxiomaticDefinitions.Prefix));
    m.put(new DefinitionId("SI", "ax", Framework.BIS), biswas(AxiomaticDefinitions.Snapshot));
    m.put(new DefinitionId("Ser", "ax", Framework.BIS), biswas(AxiomaticDefinitions.Ser));
    m.put(new DefinitionId("UpdateSer", Framework.BIS), biswas(FeketeReadOnlyAnomaly::updateSer));

    // cerone axiomatic
    m.put(new DefinitionId("RA", "ax", Framework.CER), cerone(CeroneDefinitions.RA));
    m.put(new DefinitionId("CC", "ax", Framework.CER), cerone(CeroneDefinitions.CC));
    m.put(new DefinitionId("PC", "ax", Framework.CER), cerone(CeroneDefinitions.PC));
    m.put(new DefinitionId("SI", "ax", Framework.CER), cerone(CeroneDefinitions.SI));
    m.put(new DefinitionId("Ser", "ax", Framework.CER), cerone(CeroneDefinitions.Ser));
    m.put(new DefinitionId("UpdateSer", Framework.CER), cerone(FeketeReadOnlyAnomaly::updateSer));

    // biswas tap
    m.put(
        new DefinitionId("RA", "tap", Framework.BIS),
        biswas(TransactionalAnomalousPatterns.ReadAtomic));
    m.put(
        new DefinitionId("CC", "tap", Framework.BIS),
        biswas(TransactionalAnomalousPatterns.Causal));
    return m;
  }

  private static final List<String> levels = Arrays.asList("RA", "CC", "PC", "SI", "Ser");

  private static final List<Pair<String>> edges =
      Arrays.asList(
          new Pair<>("RA", "CC"),
          new Pair<>("CC", "PC"),
          new Pair<>("PC", "SI"),
          new Pair<>("SI", "Ser"));

  public static Named<IsoldeSpec> resolve(DefinitionId pos, DefinitionId neg) {
    return resolve(Collections.singletonList(pos), neg);
  }

  public static Named<IsoldeSpec> resolve(List<DefinitionId> pos, DefinitionId neg) {
    assert !pos.isEmpty();
    StringBuilder name = new StringBuilder();
    IsoldeSpec.Builder specBuilder = new IsoldeSpec.Builder(definitions.get(pos.get(0)));
    name.append(pos.get(0));

    for (int i = 1; i < pos.size(); i++) {
      var id = pos.get(i);
      name.append(' ').append(id);
      specBuilder.and(definitions.get(id));
    }

    name.append('\t').append(neg);
    specBuilder.andNot(definitions.get(neg));
    return new Named<IsoldeSpec>(name.toString(), specBuilder.build());
  }

  public static List<Named<IsoldeSpec>> satSameFramework() {
    List<Named<IsoldeSpec>> problems = new ArrayList<>();
    for (var edge : edges) {
      var pos = edge.fst();
      var neg = edge.snd();

      // Biswas
      problems.add(
          resolve(
              new DefinitionId(pos, "ax", Framework.BIS),
              new DefinitionId(neg, "ax", Framework.BIS)));

      // Cerone
      problems.add(
          resolve(
              new DefinitionId(pos, "ax", Framework.CER),
              new DefinitionId(neg, "ax", Framework.CER)));
    }

    // Biswas read-only anomaly
    problems.add(
        resolve(
            Arrays.asList(DefinitionId.bis("SI", "ax"), DefinitionId.bis("UpdateSer")),
            DefinitionId.bis("Ser", "ax")));

    // Cerone read-only anomaly
    problems.add(
        resolve(
            Arrays.asList(DefinitionId.cer("SI", "ax"), DefinitionId.cer("UpdateSer")),
            DefinitionId.cer("Ser", "ax")));

    problems.add(resolve(DefinitionId.bis("RA", "tap"), DefinitionId.bis("RA", "ax")));

    return problems;
  }

  public static List<Named<IsoldeSpec>> satDiffFramework() {
    List<Named<IsoldeSpec>> problems = new ArrayList<>();
    for (var edge : edges) {
      var pos = edge.fst();
      var neg = edge.snd();

      problems.add(resolve(DefinitionId.bis(pos, "ax"), DefinitionId.cer(neg, "ax")));
      problems.add(resolve(DefinitionId.cer(pos, "ax"), DefinitionId.bis(neg, "ax")));
    }

    // For the read-only anomaly
    for (var siFw : Framework.values()) {
      for (var updateSerFw : Framework.values()) {
        for (var serFw : Framework.values()) {
          if (siFw != updateSerFw || updateSerFw != serFw) {
            problems.add(
                resolve(
                    Arrays.asList(
                        new DefinitionId("SI", "ax", siFw),
                        new DefinitionId("UpdateSer", updateSerFw)),
                    new DefinitionId("Ser", "ax", serFw)));
          }
        }
      }
    }

    return problems;
  }

  public static List<Named<IsoldeSpec>> unsatDiffFramework() {
    List<Named<IsoldeSpec>> problems = new ArrayList<>();
    for (String level : levels) {
      problems.add(resolve(DefinitionId.bis(level, "ax"), DefinitionId.cer(level, "ax")));
      problems.add(resolve(DefinitionId.cer(level, "ax"), DefinitionId.bis(level, "ax")));
    }
    return problems;
  }

  public static List<Named<IsoldeSpec>> unsatSameFramework() {
    List<Named<IsoldeSpec>> problems = new ArrayList<>();
    for (var level : edges) {
      var pos = level.fst();
      var neg = level.snd();

      problems.add(resolve(DefinitionId.bis(neg, "ax"), DefinitionId.bis(pos, "ax")));
      problems.add(resolve(DefinitionId.cer(neg, "ax"), DefinitionId.cer(pos, "ax")));
    }

    problems.add(resolve(DefinitionId.bis("RA", "ax"), DefinitionId.bis("RA", "tap")));
    problems.add(resolve(DefinitionId.bis("CC", "ax"), DefinitionId.bis("CC", "tap")));
    problems.add(resolve(DefinitionId.bis("CC", "tap"), DefinitionId.bis("CC", "ax")));

    return problems;
  }

  enum SpecClass {
    SAT_SAME,
    SAT_DIFF,
    UNSAT_SAME,
    UNSAT_DIFF
  }

  public static List<Named<IsoldeSpec>> getProblemSet(SpecClass specClass) {
    return switch (specClass) {
      case SAT_SAME -> satSameFramework();
      case SAT_DIFF -> satDiffFramework();
      case UNSAT_SAME -> unsatSameFramework();
      case UNSAT_DIFF -> unsatDiffFramework();
    };
  }

  public static Named<IsoldeSpec> getRepresentativeProblem(SpecClass specClass) {
    return switch (specClass) {
      case UNSAT_SAME -> resolve(DefinitionId.bis("CC", "ax"), DefinitionId.bis("CC", "tap"));
      case UNSAT_DIFF -> resolve(DefinitionId.bis("RA", "ax"), DefinitionId.cer("RA", "ax"));
      case SAT_SAME ->
          resolve(
              Arrays.asList(DefinitionId.bis("SI", "ax"), DefinitionId.bis("UpdateSer")),
              DefinitionId.bis("Ser", "ax"));
      case SAT_DIFF ->
          resolve(
              Arrays.asList(DefinitionId.bis("SI", "ax"), DefinitionId.cer("UpdateSer")),
              DefinitionId.cer("Ser", "ax"));
    };
  }
}
