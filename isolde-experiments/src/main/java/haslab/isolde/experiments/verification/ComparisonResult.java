package haslab.isolde.experiments.verification;

import haslab.isolde.SynthesizedHistory;
import haslab.isolde.core.synth.Scope;

public record ComparisonResult(
    String a, String b, Scope scope, SynthesizedHistory a_not_b, SynthesizedHistory b_not_a) {

  @Override
  public final String toString() {
    String str;
    if (b_not_a.sat() && a_not_b.sat()) {
      // incomparable
      str =
          String.format(
              "%s and %s are INCOMPARABLE.\n"
                  + "History allowed by %s but not by %s:\n"
                  + "%s\n\n"
                  + "History allowed by %s but not by %s:\n"
                  + "%s\n"
                  + " ",
              a, b, a, b, a_not_b.history(), b, a, b_not_a.history());
    } else if (b_not_a.sat() && a_not_b.unsat()) {
      // A is stronger than B
      str =
          String.format(
              "%s is STRONGER than %s.\n" + "History allowed by %s but not by %s:\n" + "%s\n",
              a, b, b, a, b_not_a.history());
    } else if (a_not_b.sat() && b_not_a.unsat()) {
      // B is stronger than A
      str =
          String.format(
              "%s is STRONGER than %s.\n" + "History allowed by %s but not by %s:\n" + "%s\n",
              b, a, a, b, a_not_b.history());
    } else if (a_not_b.sat() && b_not_a.sat()) {
      // equivalent
      str = String.format("%s and %s are EQUIVALENT.\n", a, b);
    } else {
      str = String.format("what");
    }
    return str;
  }

  public String timeInfoString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("Using %s.\n", scope));
    sb.append(String.format("Time for synthesizing %s and not %s: %d ms.\n", a, b, a_not_b.time()));
    sb.append(String.format("Time for synthesizing %s and not %s: %d ms.\n", b, a, b_not_a.time()));
    sb.append("Total comparison time: ").append(a_not_b.time() + b_not_a.time()).append(" ms.");
    return sb.toString();
  }
}
