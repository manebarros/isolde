package haslab.isolde.experiments.benchmark;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.HistoryExpression;
import haslab.isolde.core.HistorySchema;
import kodkod.ast.Formula;

/**
 * UpdateSer isolation-level definitions used by the read-only-anomaly benchmark problems.
 *
 * <p>UpdateSer is Serializability restricted to the update transactions (those that write the final
 * value of some key), so read-only transactions are exempt. These definitions are the live half of
 * the old {@code verification.FeketeReadOnlyAnomaly} demo; they live here so the benchmark catalog
 * ({@link Problems}) depends only on definitions, not on that demo code.
 */
public final class UpdateSerDefinitions {

  private UpdateSerDefinitions() {}

  /** The update transactions: those whose writes are the final value of some key. */
  public static final HistoryExpression updateTransactions = HistorySchema::updateTransactions;

  public static Formula updateSer(BiswasExecution e) {
    return AxiomaticDefinitions.Serializability(
        new BiswasExecution(e.history().subHistory(updateTransactions), e.co()));
  }

  public static Formula updateSer(CeroneExecution e) {
    return CeroneDefinitions.EXT
        .resolve(e)
        .and(
            CeroneDefinitions.TOTAL_VIS.resolve(
                new CeroneExecution(e.history().subHistory(updateTransactions), e.vis(), e.ar())));
  }
}
