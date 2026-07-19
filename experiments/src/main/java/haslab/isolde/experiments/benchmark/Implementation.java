package haslab.isolde.experiments.benchmark;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.IsoldeSynthesizer;
import haslab.isolde.NaiveIsoldeSynthesizer;
import haslab.isolde.SynthesizerI;
import haslab.isolde.core.synth.Scope;
import kodkod.engine.config.Options;

public enum Implementation implements SynthesisRunner {
  CEGIS_ALL("all", new IsoldeSynthesizer.Builder().build()),

  CEGIS_NO_SMART_SEARCH(
      "no_smart_search", new IsoldeSynthesizer.Builder().smartCandidateSearch(false).build()),

  CEGIS_NO_FIXED_COMMIT_ORDER(
      "no_fixed_co", new IsoldeSynthesizer.Builder().useTxnTotalOrder(false).build()),

  CEGIS_NO_INC_SOLVING(
      "no_incremental", new IsoldeSynthesizer.Builder().incrementalSolving(false).build()),

  CEGIS_NONE(
      "none",
      new IsoldeSynthesizer.Builder()
          .smartCandidateSearch(false)
          .useTxnTotalOrder(false)
          .incrementalSolving(false)
          .build()),

  NO_LEARNING("no_learning", new NaiveIsoldeSynthesizer());

  private String id;
  private SynthesizerI synthesizer;

  private Implementation(String id, SynthesizerI synthesizer) {
    this.id = id;
    this.synthesizer = synthesizer;
  }

  public String getId() {
    return id;
  }

  public SynthesizerI getSynthesizer() {
    return synthesizer;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public boolean supports(IsoldeSpec spec) {
    return true;
  }

  @Override
  public SynthesisOutcome run(Scope scope, IsoldeSpec spec, Options options) {
    return new CegisOutcome(synthesizer.synthesize(scope, spec, options));
  }
}
