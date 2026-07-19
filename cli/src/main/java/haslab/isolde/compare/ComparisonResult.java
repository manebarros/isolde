package haslab.isolde.compare;

import haslab.isolde.SynthesizedHistory;
import haslab.isolde.core.synth.Scope;

public record ComparisonResult(
    String a, String b, Scope scope, SynthesizedHistory a_not_b, SynthesizedHistory b_not_a) {

  public enum Verdict {
    EQUIVALENT,
    A_STRONGER,
    B_STRONGER,
    INCOMPARABLE
  }

  /**
   * Classifies the comparison from the two directions: a history allowed by {@code a} but not
   * {@code b} means {@code a} is not the stronger one, and symmetrically for the other direction.
   */
  public Verdict verdict() {
    if (a_not_b.sat() && b_not_a.sat()) return Verdict.INCOMPARABLE;
    if (a_not_b.unsat() && b_not_a.sat()) return Verdict.A_STRONGER;
    if (a_not_b.sat() && b_not_a.unsat()) return Verdict.B_STRONGER;
    return Verdict.EQUIVALENT;
  }

  public boolean isEquivalent() {
    return verdict() == Verdict.EQUIVALENT;
  }

  @Override
  public String toString() {
    String str =
        switch (verdict()) {
          case INCOMPARABLE ->
              String.format(
                  "%s and %s are INCOMPARABLE.\n"
                      + "History allowed by %s but not by %s:\n"
                      + "%s\n\n"
                      + "History allowed by %s but not by %s:\n"
                      + "%s\n ",
                  a, b, a, b, a_not_b.history(), b, a, b_not_a.history());
          case A_STRONGER ->
              String.format(
                  "%s is STRONGER than %s.\n" + "History allowed by %s but not by %s:\n" + "%s\n",
                  a, b, b, a, b_not_a);
          case B_STRONGER ->
              String.format(
                  "%s is STRONGER than %s.\n" + "History allowed by %s but not by %s:\n" + "%s\n",
                  b, a, a, b, a_not_b);
          case EQUIVALENT -> String.format("%s and %s are EQUIVALENT.\n", a, b);
        };
    return str + timeInfoString();
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
