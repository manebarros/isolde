package haslab.isolde.experiments.benchmark;

import haslab.isolde.SynthesizedHistory;

/** Adapts a CEGIS/naive {@link SynthesizedHistory} to the neutral {@link SynthesisOutcome}. */
public final class CegisOutcome implements SynthesisOutcome {
  private final SynthesizedHistory history;

  public CegisOutcome(SynthesizedHistory history) {
    this.history = history;
  }

  @Override
  public boolean sat() {
    return history.sat();
  }

  @Override
  public long totalTimeMillis() {
    return history.time();
  }

  @Override
  public int candidates() {
    return history.candidates();
  }

  @Override
  public long synthTimeMillis() {
    return history.cegisResult().getSynthTime();
  }

  @Override
  public long checkTimeMillis() {
    return history.cegisResult().getCheckTime();
  }

  @Override
  public int initialSynthClauses() {
    return history.cegisResult().getInitialSynthClauses();
  }

  @Override
  public int finalSynthClauses() {
    return history.cegisResult().getFinalSynthClauses();
  }
}
