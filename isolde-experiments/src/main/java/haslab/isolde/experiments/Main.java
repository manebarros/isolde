package haslab.isolde.experiments;

import static haslab.isolde.IsoldeConstraint.biswas;
import static haslab.isolde.IsoldeConstraint.cerone;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.IsoldeSynthesizer;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.experiments.benchmark.IsoldeConfiguration;
import haslab.isolde.experiments.benchmark.IsoldeConfiguration.NamedProblem;
import haslab.isolde.experiments.benchmark.Util;
import haslab.isolde.experiments.verification.FeketeReadOnlyAnomaly;
import haslab.isolde.experiments.verification.VerifyPlumeDefinitions;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main {
  private static void plume() {
    Scope s = new Scope(4, 2, 3, 2);
    VerifyPlumeDefinitions.verify(s);
  }

  private static void benchmark() throws IOException {

    IsoldeConfiguration full = new IsoldeConfiguration("default", new IsoldeSynthesizer.Builder());
    IsoldeConfiguration withoutIncremental =
        new IsoldeConfiguration(
            "without incremental", new IsoldeSynthesizer.Builder().incrementalSolving(false));
    IsoldeConfiguration withoutFixedTotalOrder =
        new IsoldeConfiguration(
            "without fixed order", new IsoldeSynthesizer.Builder().useTxnTotalOrder(false));
    IsoldeConfiguration withNaiveSearch =
        new IsoldeConfiguration(
            "without smart search", new IsoldeSynthesizer.Builder().smartCandidateSearch(false));

    List<IsoldeConfiguration> configs =
        // Arrays.asList(full, withoutIncremental, withoutFixedTotalOrder, withNaiveSearch);
        Arrays.asList(withoutFixedTotalOrder, withNaiveSearch);

    List<Scope> scopes = Util.scopesFromRange(5, 5, 5, 6, 7);

    IsoldeSpec fekete =
        new IsoldeSpec.Builder(cerone(CeroneDefinitions.SI))
            .and(cerone(FeketeReadOnlyAnomaly::updateSer))
            .andNot(cerone(CeroneDefinitions.SER))
            .build();

    IsoldeSpec siEquivalence =
        new IsoldeSpec.Builder(cerone(CeroneDefinitions.SI))
            .andNot(biswas(AxiomaticDefinitions.Snapshot))
            .build();

    IsoldeSpec satDifFrameworks =
        new IsoldeSpec.Builder(cerone(CeroneDefinitions.SI))
            .andNot(biswas(AxiomaticDefinitions.Ser))
            .build();

    List<NamedProblem> problems =
        Arrays.asList(
            // new NamedProblem(fekete, "fekete"),
            // new NamedProblem(satDifFrameworks, "satDifFrameworks"),
            new NamedProblem(siEquivalence, "siEquivalence"));

    for (var config : configs) {
      config.measureAndAppend(
          scopes,
          problems,
          Collections.singletonList("glucose"),
          3,
          "/home/mane/vldb_measurements/data.csv");
    }
  }

  public static void main(String[] args) throws IOException {
    FeketeReadOnlyAnomaly.generateAnomalyBiswas();
  }
}
