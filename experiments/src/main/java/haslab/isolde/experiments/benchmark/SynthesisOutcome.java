package haslab.isolde.experiments.benchmark;

/**
 * A synthesizer-agnostic view of one synthesis run, so the benchmark harness can measure any
 * synthesizer uniformly.
 *
 * <p>The CEGIS-specific metrics default to {@code -1}, which {@link Measurement#num} renders as an
 * empty CSV cell — appropriate for synthesizers (e.g. the exhaustive enumerator) that have no
 * synthesis SAT formula and no synth/check phase split.
 */
public interface SynthesisOutcome {
  boolean sat();

  long totalTimeMillis();

  int candidates();

  default long synthTimeMillis() {
    return -1;
  }

  default long checkTimeMillis() {
    return -1;
  }

  default int initialSynthClauses() {
    return -1;
  }

  default int finalSynthClauses() {
    return -1;
  }
}
