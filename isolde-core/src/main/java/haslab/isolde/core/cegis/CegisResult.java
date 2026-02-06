package haslab.isolde.core.cegis;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.history.History;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import kodkod.ast.Formula;
import kodkod.instance.Instance;

public class CegisResult {
  private final AbstractHistoryK historyEncoding;
  private final Optional<Instance> solution;
  private final List<FailedCandidate> failedCandidates;
  private final int initialSynthClauses;
  private final int finalSynthClauses;
  private final long synthTime;
  private final long checkTime;
  private final long totalTime;

  public static record FailedCandidate(
      Instance instance, List<? extends Counterexample<?>> counterexamples) {}

  public static record Counterexample<E extends Execution>(
      Instance instance, E execution, ExecutionFormula<E> formula) {
    public Formula rawFormula() {
      return formula.resolve(execution);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(new History(execution.history(), instance));
      sb.append(execution.showAdditionalStructures(instance)).append("\n");
      return sb.toString();
    }
  }

  private CegisResult(
      AbstractHistoryK historyEncoding,
      Instance solution,
      List<FailedCandidate> failedCandidates,
      int initialSynthClauses,
      int finalSynthClauses,
      long synthTime,
      long checkTime,
      long totalTime) {
    this.historyEncoding = historyEncoding;
    this.solution = Optional.ofNullable(solution);
    this.failedCandidates = Collections.unmodifiableList(failedCandidates);
    this.initialSynthClauses = initialSynthClauses;
    this.finalSynthClauses = finalSynthClauses;
    this.synthTime = synthTime;
    this.checkTime = checkTime;
    this.totalTime = totalTime;
  }

  public static CegisResult success(
      AbstractHistoryK historyEncoding,
      Instance instance,
      List<FailedCandidate> failedCandidates,
      int initialSynthClauses,
      int finalSynthClauses,
      long synthTime,
      long checkTime,
      long totalTime) {
    assert instance != null;
    return new CegisResult(
        historyEncoding,
        instance,
        failedCandidates,
        initialSynthClauses,
        finalSynthClauses,
        synthTime,
        checkTime,
        totalTime);
  }

  public static CegisResult fail(
      AbstractHistoryK historyEncoding,
      List<FailedCandidate> failedCandidates,
      int initialSynthClauses,
      int finalSynthClauses,
      long synthTime,
      long checkTime,
      long totalTime) {
    return new CegisResult(
        historyEncoding,
        null,
        failedCandidates,
        initialSynthClauses,
        finalSynthClauses,
        synthTime,
        checkTime,
        totalTime);
  }

  public History history() {
    return new History(historyEncoding, solution.get());
  }

  public Optional<Instance> getSolution() {
    return solution;
  }

  public List<FailedCandidate> getFailedCandidates() {
    return failedCandidates;
  }

  public AbstractHistoryK getHistoryEncoding() {
    return historyEncoding;
  }

  public boolean sat() {
    return this.solution.isPresent();
  }

  public int candidatesNr() {
    return sat() ? failedCandidates.size() + 1 : failedCandidates.size();
  }

  public int getInitialSynthClauses() {
    return initialSynthClauses;
  }

  public int getFinalSynthClauses() {
    return finalSynthClauses;
  }

  public long getSynthTime() {
    return synthTime;
  }

  public long getCheckTime() {
    return checkTime;
  }

  public long getTotalTime() {
    return totalTime;
  }
}
