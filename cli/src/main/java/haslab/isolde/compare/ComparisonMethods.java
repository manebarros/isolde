package haslab.isolde.compare;

import static haslab.isolde.IsoldeConstraint.biswas;
import static haslab.isolde.IsoldeConstraint.cerone;
import static haslab.isolde.IsoldeConstraint.history;

import haslab.isolde.IsoldeConstraint;
import haslab.isolde.IsoldeSpec;
import haslab.isolde.IsoldeSynthesizer;
import haslab.isolde.SynthesizedHistory;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.synth.Scope;
import kodkod.engine.satlab.SATFactory;

public final class ComparisonMethods {
  private ComparisonMethods() {}

  /**
   * Compares two definitions by synthesizing a history allowed by {@code a} but not {@code b} and
   * vice versa; the resulting {@link ComparisonResult} classifies them as equivalent, one stronger,
   * or incomparable (up to the given scope). Works for any framework combination because {@link
   * IsoldeConstraint} unifies biswas/cerone/history constraints.
   */
  public static ComparisonResult compare(
      Scope scope, String nameA, IsoldeConstraint a, String nameB, IsoldeConstraint b) {
    return compare(scope, nameA, a, nameB, b, SATFactory.DefaultSAT4J);
  }

  public static ComparisonResult compare(
      Scope scope,
      String nameA,
      IsoldeConstraint a,
      String nameB,
      IsoldeConstraint b,
      SATFactory solver) {
    IsoldeSynthesizer synthesizer = new IsoldeSynthesizer.Builder().solver(solver).build();
    SynthesizedHistory aNotB = synthesizer.synthesize(scope, a.andNot(b).build());
    SynthesizedHistory bNotA = synthesizer.synthesize(scope, b.andNot(a).build());
    return new ComparisonResult(nameA, nameB, scope, aNotB, bNotA);
  }

  private static IsoldeSynthesizer synthesizer() {
    return new IsoldeSynthesizer.Builder().build();
  }

  public static ComparisonResult compareBiswasCerone(
      Scope scope,
      String a_name,
      ExecutionFormula<CeroneExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {

    IsoldeSynthesizer synthesizer = new IsoldeSynthesizer.Builder().build();

    IsoldeSpec spec = cerone(a_def).andNot(biswas(b_def)).build();
    SynthesizedHistory a_not_b = synthesizer.synthesize(scope, spec);

    spec = biswas(b_def).andNot(cerone(a_def)).build();
    SynthesizedHistory b_not_a = synthesizer.synthesize(scope, spec);

    return new ComparisonResult(a_name, b_name, scope, a_not_b, b_not_a);
  }

  public static ComparisonResult compareBiswas(
      Scope scope,
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {

    IsoldeSynthesizer synthesizer = synthesizer();

    var spec = biswas(a_def).andNot(biswas(b_def)).build();
    SynthesizedHistory a_not_b = synthesizer.synthesize(scope, spec);

    spec = biswas(b_def).andNot(biswas(a_def)).build();
    SynthesizedHistory b_not_a = synthesizer.synthesize(scope, spec);

    return new ComparisonResult(a_name, b_name, scope, a_not_b, b_not_a);
  }

  public static ComparisonResult compareBiswasExecution(
      Scope scope,
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      ExecutionFormula<BiswasExecution> b_def) {

    var synthesizer = synthesizer();

    var spec = biswas(a_def.and(b_def.not())).asSpec();
    var a_not_b = synthesizer.synthesize(scope, spec);

    spec = biswas(b_def.and(a_def.not())).asSpec();
    var b_not_a = synthesizer.synthesize(scope, spec);

    return new ComparisonResult(a_name, b_name, scope, a_not_b, b_not_a);
  }

  public static ComparisonResult compareBiswasAndHistoryBased(
      Scope scope,
      String a_name,
      ExecutionFormula<BiswasExecution> a_def,
      String b_name,
      HistoryFormula b_def) {

    var synthesizer = synthesizer();

    var spec = biswas(a_def).andNot(history(b_def)).build();
    var a_not_b = synthesizer.synthesize(scope, spec);

    spec = history(b_def).andNot(biswas(a_def)).build();
    var b_not_a = synthesizer.synthesize(scope, spec);

    return new ComparisonResult(a_name, b_name, scope, a_not_b, b_not_a);
  }

  public static ComparisonResult compareCerone(
      Scope scope,
      String a_name,
      ExecutionFormula<CeroneExecution> a_def,
      String b_name,
      ExecutionFormula<CeroneExecution> b_def) {
    IsoldeSynthesizer synthesizer = synthesizer();

    var spec = cerone(a_def).andNot(cerone(b_def)).build();
    SynthesizedHistory a_not_b = synthesizer.synthesize(scope, spec);

    spec = cerone(b_def).andNot(cerone(a_def)).build();
    SynthesizedHistory b_not_a = synthesizer.synthesize(scope, spec);

    return new ComparisonResult(a_name, b_name, scope, a_not_b, b_not_a);
  }
}
