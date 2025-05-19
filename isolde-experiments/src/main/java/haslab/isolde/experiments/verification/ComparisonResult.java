package haslab.isolde.experiments.verification;

import haslab.isolde.history.History;
import java.util.Optional;

public record ComparisonResult(
    String a, String b, Optional<History> a_not_b, Optional<History> b_not_a) {

  @Override
  public final String toString() {
    String str;
    if (b_not_a.isPresent() && a_not_b.isPresent()) {
      // incomparable
      str =
          String.format(
              "%s and %s are INCOMPARABLE.\n"
                  + "History allowed by %s but not by %s:\n"
                  + "%s\n\n"
                  + "History allowed by %s but not by %s:\n"
                  + "%s\n"
                  + " ",
              a, b, a, b, a_not_b.get(), b, a, b_not_a.get());
    } else if (b_not_a.isPresent() && a_not_b.isEmpty()) {
      // A is stronger than B
      str =
          String.format(
              "%s is STRONGER than %s.\n" + "History allowed by %s but not by %s:\n" + "%s\n",
              a, b, b, a, b_not_a.get());
    } else if (a_not_b.isPresent() && b_not_a.isEmpty()) {
      // B is stronger than A
      str =
          String.format(
              "%s is STRONGER than %s.\n" + "History allowed by %s but not by %s:\n" + "%s\n",
              b, a, a, b, a_not_b.get());
    } else if (a_not_b.isEmpty() && b_not_a.isEmpty()) {
      // equivalent
      str = String.format("%s and %s are EQUIVALENT.\n", a, b);
    } else {
      str = String.format("what");
    }
    return str;
  }
}
