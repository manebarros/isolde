package haslab.isolde.experiments.benchmark;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.WRITE;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.synth.Scope;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kodkod.engine.satlab.SATFactory;

public final class Util {
  public static final Map<String, SATFactory> solvers =
      Map.of(
          "minisat", SATFactory.MiniSat,
          "glucose", SATFactory.Glucose,
          "sat4j", SATFactory.DefaultSAT4J);

  public static record LevelDefinitions(
      String name,
      ExecutionFormula<CeroneExecution> ceroneDef,
      ExecutionFormula<BiswasExecution> biswasDef) {}

  public static final Map<String, LevelDefinitions> levels =
      Map.of(
          "RA", new LevelDefinitions("RA", CeroneDefinitions.RA, AxiomaticDefinitions.ReadAtomic),
          "CC", new LevelDefinitions("CC", CeroneDefinitions.CC, AxiomaticDefinitions.Causal),
          "PC", new LevelDefinitions("PC", CeroneDefinitions.PC, AxiomaticDefinitions.Prefix),
          "SI", new LevelDefinitions("Ser", CeroneDefinitions.SER, AxiomaticDefinitions.Ser),
          "Ser", new LevelDefinitions("SI", CeroneDefinitions.SI, AxiomaticDefinitions.Snapshot));

  private static void writeString(String s, Path p) throws IOException {
    Path dir = p.getParent();
    if (!Files.exists(dir)) Files.createDirectories(dir);
    Files.writeString(p, s);
  }

  private static void appendString(String s, Path p) throws IOException {
    assert Files.exists(p);
    Files.writeString(p, s, APPEND, WRITE);
  }

  public static void writeMeasurements(List<Measurement> measurements, String p)
      throws IOException {
    writeMeasurements(measurements, Path.of(p));
  }

  public static void writeMeasurements(List<Measurement> measurements, Path p) throws IOException {
    writeString(Measurement.asCsv(measurements), p);
  }

  public static void appendMeasurements(List<Measurement> measurements, String p)
      throws IOException {
    appendMeasurements(measurements, Path.of(p));
  }

  public static void appendMeasurements(List<Measurement> measurements, Path p) throws IOException {
    if (Files.exists(p)) {
      appendString(Measurement.asCsvWithoutHeader(measurements), p);
    } else {
      writeMeasurements(measurements, p);
    }
  }

  public static <R> String unlines(List<R> rows) {
    StringBuilder sb = new StringBuilder();
    for (var row : rows) sb.append(row).append("\n");
    return sb.toString();
  }

  public static List<Scope> scopesFromRange(int keys, int val, int sessions, int from, int to) {
    return scopesFromRange(keys, val, sessions, from, to, 1);
  }

  public static List<Scope> scopesFromRange(
      int keys, int val, int sessions, int from, int to, int step) {
    List<Scope> scopes = new ArrayList<>();
    for (int txn_num = from; txn_num <= to; txn_num += step) {
      scopes.add(new Scope(txn_num, keys, val, sessions));
    }
    return scopes;
  }

  public static List<Scope> scopesFromRangeWithoutSessions(int keys, int val, int from, int to) {
    return scopesFromRangeWithoutSessions(keys, val, from, to, 1);
  }

  public static List<Scope> scopesFromRangeWithoutSessions(
      int keys, int val, int from, int to, int step) {
    List<Scope> scopes = new ArrayList<>();
    for (int txn_num = from; txn_num <= to; txn_num += step) {
      scopes.add(new Scope(txn_num, keys, val, txn_num));
    }
    return scopes;
  }
}
