package haslab.isolde;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.history.History;
import java.util.List;
import kodkod.engine.Solution;
import kodkod.instance.Instance;

public class SynthesizedHistory {
  private final List<Solution> candidates;
  private final AbstractHistoryK historyEncoding;
  private final List<CeroneExecution> ceroneExecutions;
  private final List<BiswasExecution> biswasExecutions;

  public SynthesizedHistory(
      List<Solution> candidates,
      AbstractHistoryK historyEncoding,
      List<CeroneExecution> ceroneExecutions,
      List<BiswasExecution> biswasExecutions) {
    this.candidates = candidates;
    this.historyEncoding = historyEncoding;
    this.ceroneExecutions = ceroneExecutions;
    this.biswasExecutions = biswasExecutions;
  }

  public boolean sat() {
    return candidates.getFirst().sat();
  }

  @Override
  public String toString() {
    if (!sat()) return "No History";
    Instance instance = candidates.getFirst().instance();
    StringBuilder sb = new StringBuilder();
    sb.append(new History(historyEncoding, instance));
    sb.append("\n\n");
    for (var exec : ceroneExecutions) {
      sb.append(exec.showAdditionalStructures(instance)).append("\n");
    }
    for (var exec : biswasExecutions) {
      sb.append(exec.showAdditionalStructures(instance)).append("\n");
    }
    return sb.toString();
  }

  public String showWithFirstCerone() {
    if (!sat()) return "No History";
    Instance instance = candidates.getFirst().instance();
    StringBuilder sb = new StringBuilder();
    sb.append(new History(historyEncoding, instance));
    sb.append("\n");
    sb.append(ceroneExecutions.get(0).showAdditionalStructures(instance));
    return sb.toString();
  }
}
