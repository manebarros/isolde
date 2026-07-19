package haslab.isolde.cli;

import haslab.isolde.IsoldeConstraint;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;

/**
 * Resolves a {@code FRAMEWORK:LEVEL} string (e.g. {@code biswas:Ser}, {@code cerone:UpdateSer}) into
 * an {@link IsoldeConstraint}. LEVEL is the name of a {@code public static} {@link ExecutionFormula}
 * field on {@link AxiomaticDefinitions} (biswas) or {@link CeroneDefinitions} (cerone). Shared by the
 * synthesis command and the {@code compare} command so both understand the same level catalog.
 */
final class Constraints {
  private Constraints() {}

  private enum Framework {
    BISWAS,
    CERONE
  }

  static IsoldeConstraint parse(String spec) {
    int colon = spec.indexOf(':');
    if (colon <= 0 || colon == spec.length() - 1) {
      throw new IllegalArgumentException(
          "expected FRAMEWORK:LEVEL (e.g. biswas:Ser), got: " + spec);
    }
    Framework framework = Framework.valueOf(spec.substring(0, colon).toUpperCase());
    String level = spec.substring(colon + 1);
    return switch (framework) {
      case BISWAS ->
          IsoldeConstraint.biswas(
              Constraints.<BiswasExecution>lookup(AxiomaticDefinitions.class, level));
      case CERONE ->
          IsoldeConstraint.cerone(
              Constraints.<CeroneExecution>lookup(CeroneDefinitions.class, level));
    };
  }

  @SuppressWarnings("unchecked")
  private static <E extends Execution> ExecutionFormula<E> lookup(
      Class<?> definitions, String fieldName) {
    try {
      return (ExecutionFormula<E>) definitions.getField(fieldName).get(null);
    } catch (NoSuchFieldException e) {
      throw new IllegalArgumentException(
          "no such isolation level: " + fieldName + " on " + definitions.getSimpleName());
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
