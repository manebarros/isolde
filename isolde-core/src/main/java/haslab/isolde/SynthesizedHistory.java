package haslab.isolde;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.core.cegis.CegisResult;
import haslab.isolde.history.History;
import java.util.List;
import kodkod.instance.Instance;

public class SynthesizedHistory {
  private final CegisResult cegisResult;
  private final List<CeroneExecution> ceroneExecutions;
  private final List<BiswasExecution> biswasExecutions;

  public SynthesizedHistory(
      CegisResult cegisResult,
      List<CeroneExecution> ceroneExecutions,
      List<BiswasExecution> biswasExecutions) {
    this.cegisResult = cegisResult;
    this.ceroneExecutions = ceroneExecutions;
    this.biswasExecutions = biswasExecutions;
  }

  public boolean sat() {
    return cegisResult.sat();
  }

  public boolean unsat() {
    return !sat();
  }

  public long time() {
    return this.cegisResult.getTime();
  }

  public History history() {
    return cegisResult.history();
  }

  public int candidates() {
    return this.cegisResult.candidatesNr();
  }

  public CegisResult cegisResult() {
    return this.cegisResult;
  }

  @Override
  public String toString() {
    if (!sat()) return "No History";
    Instance instance = cegisResult.getSolution().get();
    StringBuilder sb = new StringBuilder();
    sb.append(new History(cegisResult.getHistoryEncoding(), instance));
    sb.append("\n\n");
    var count = 1;
    if (ceroneExecutions != null) {
      for (var exec : ceroneExecutions) {
        sb.append(String.format("Execution #%d:\n", count++));
        sb.append(exec.showAdditionalStructures(instance)).append("\n");
      }
    }
    if (biswasExecutions != null) {
      for (var exec : biswasExecutions) {
        sb.append(String.format("Execution #%d:\n", count++));
        sb.append(exec.showAdditionalStructures(instance)).append("\n");
      }
    }
    return sb.toString();
  }
}
