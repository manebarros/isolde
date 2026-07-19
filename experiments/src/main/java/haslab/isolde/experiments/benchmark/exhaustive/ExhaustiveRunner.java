package haslab.isolde.experiments.benchmark.exhaustive;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.experiments.benchmark.SynthesisOutcome;
import haslab.isolde.experiments.benchmark.SynthesisRunner;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.engine.config.Options;

/**
 * Runs the brute-force exhaustive enumerator as a benchmark baseline. It is Biswas-only, so it
 * declines Cerone problems. Decomposes an {@link IsoldeSpec} into the positive "allowed-by" formulas
 * and the single forbidden one the enumerator needs.
 */
public final class ExhaustiveRunner implements SynthesisRunner {

  @Override
  public String id() {
    return "exhaustive";
  }

  @Override
  public boolean supports(IsoldeSpec spec) {
    return spec.usesBiswas() && !spec.usesCerone();
  }

  @Override
  public SynthesisOutcome run(Scope scope, IsoldeSpec spec, Options options) {
    SynthesisSpec<BiswasExecution> biswas = spec.getBiswasSpec().orElseThrow();
    List<ExecutionFormula<BiswasExecution>> pos = biswas.existentialFormulas();
    // The spec stores the forbidden level already negated (andNot records formula.not()); negate
    // again to recover it, so the enumerator's "not allowed by neg" check is correct.
    ExecutionFormula<BiswasExecution> neg =
        biswas
            .universalFormula()
            .map(u -> (ExecutionFormula<BiswasExecution>) (e -> u.resolve(e).not()))
            .orElse(e -> Formula.FALSE);
    Synthesizer.SynthesisSolution solution =
        new Synthesizer(options.solver()).synthesize(scope, pos, neg);
    return new ExhaustiveOutcome(solution);
  }
}
