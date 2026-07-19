package haslab.isolde.experiments.benchmark.exhaustive;

import haslab.isolde.experiments.benchmark.SynthesisOutcome;

/**
 * Adapts the exhaustive enumerator's {@link Synthesizer.SynthesisSolution} to {@link
 * SynthesisOutcome}. The enumerator has no synthesis SAT formula or synth/check phase split, so the
 * CEGIS-specific metrics keep their {@code -1} (empty-cell) defaults.
 */
public final class ExhaustiveOutcome implements SynthesisOutcome {
  private final Synthesizer.SynthesisSolution solution;

  public ExhaustiveOutcome(Synthesizer.SynthesisSolution solution) {
    this.solution = solution;
  }

  @Override
  public boolean sat() {
    return solution.history().isPresent();
  }

  @Override
  public long totalTimeMillis() {
    return solution.time_ms();
  }

  @Override
  public int candidates() {
    return solution.candidates();
  }
}
