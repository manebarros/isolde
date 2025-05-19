package haslab.isolde.experiments.verification;

import haslab.isolde.Synthesizer;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.History;
import java.util.Optional;

public final class ComparisonMethods {
  private ComparisonMethods() {}

  public static ComparisonResult compareBiswasCerone(
      Scope scope,
      String a_name,
      ExecutionFormula<CeroneExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {
    Synthesizer synth = new Synthesizer(scope);
    synth.registerCerone(new SynthesisSpec<>(a_def));
    synth.registerBiswas(SynthesisSpec.fromUniversal(b_def.not()));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(b_def));
    synth.registerCerone(SynthesisSpec.fromUniversal(a_def.not()));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }

  public static ComparisonResult compareBiswas(
      Scope scope,
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {
    Synthesizer synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(a_def, b_def.not()));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(b_def, a_def.not()));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }

  public static ComparisonResult compareBiswasExecution(
      Scope scope,
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {
    Synthesizer synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(a_def.and(b_def.not())));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope);
    synth.registerBiswas(new SynthesisSpec<>(b_def.and(a_def.not())));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }

  public static ComparisonResult compareBiswasAndHistoryBased(
      Scope scope,
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      HistoryFormula b_def) {
    Synthesizer synth = new Synthesizer(scope, b_def.not());
    synth.registerBiswas(new SynthesisSpec<>(a_def));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope, b_def);
    synth.registerBiswas(SynthesisSpec.fromUniversal(a_def.not()));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }

  public static ComparisonResult compareCerone(
      Scope scope,
      String a_name,
      ExecutionFormula<CeroneExecution> a_def,
      String b_name,
      ExecutionFormula<CeroneExecution> b_def) {
    Synthesizer synth = new Synthesizer(scope);
    synth.registerCerone(new SynthesisSpec<>(a_def, b_def.not()));
    Optional<History> a_not_b = synth.synthesize();

    synth = new Synthesizer(scope);
    synth.registerCerone(new SynthesisSpec<>(b_def, a_def.not()));
    Optional<History> b_not_a = synth.synthesize();

    return new ComparisonResult(a_name, b_name, a_not_b, b_not_a);
  }
}
