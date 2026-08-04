package haslab.isolde.cli;

import haslab.isolde.IsoldeConstraint;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.biswas.definitions.TransactionalAnomalousPatterns;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.cerone.definitions.CustomDefinitions;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves a {@code FRAMEWORK:LEVEL} string (e.g. {@code biswas:Ser}, {@code cerone:UpdateSer})
 * into an {@link IsoldeConstraint}. LEVEL is the name of a {@code public static} {@link
 * ExecutionFormula} field, over the framework's execution type, on one of the catalog classes
 * registered for FRAMEWORK, which is what makes a newly added definition visible to the CLI without
 * further wiring. A non-public field is deliberately invisible, which is how {@link
 * TransactionalAnomalousPatterns} keeps its individual patterns off the command line. Shared by the
 * synthesis command, the {@code compare} command and the {@code levels} command, so all three
 * understand the same catalog.
 */
final class Constraints {
  private Constraints() {}

  /**
   * A framework, the execution type its definitions are over, and the classes searched for LEVEL. A
   * framework can have more than one catalog: Plume's transactional anomalous patterns are
   * specified in the framework of Biswas and Enea, so they are {@code biswas:} levels and can be
   * compared directly against the axiomatic definitions.
   */
  enum Framework {
    BISWAS(BiswasExecution.class, AxiomaticDefinitions.class, TransactionalAnomalousPatterns.class),
    CERONE(CeroneExecution.class, CeroneDefinitions.class, CustomDefinitions.class);

    private final Class<? extends Execution> execution;
    private final List<Class<?>> catalogs;

    Framework(Class<? extends Execution> execution, Class<?>... catalogs) {
      this.execution = execution;
      this.catalogs = List.of(catalogs);
    }

    String prefix() {
      return name().toLowerCase();
    }
  }

  /** A level of the catalog: its formula, and the class that declares it. */
  record Level(Class<?> catalog, ExecutionFormula<?> formula) {}

  static IsoldeConstraint parse(String spec) {
    int colon = spec.indexOf(':');
    if (colon <= 0 || colon == spec.length() - 1) {
      throw new IllegalArgumentException(
          "expected FRAMEWORK:LEVEL (e.g. biswas:Ser), got: " + spec);
    }
    Framework framework = framework(spec.substring(0, colon));
    ExecutionFormula<?> formula = lookup(framework, spec.substring(colon + 1));
    return switch (framework) {
      case BISWAS -> IsoldeConstraint.biswas(unchecked(formula));
      case CERONE -> IsoldeConstraint.cerone(unchecked(formula));
    };
  }

  /**
   * The levels available under {@code framework}, keyed by name, in catalog and then declaration
   * order. Two catalogs of one framework declaring the same name would leave one of them
   * unnameable; for a tool whose purpose is telling near-identical definitions apart that is worse
   * than a failure, so it is one.
   */
  static Map<String, Level> levels(Framework framework) {
    Map<String, Level> levels = new LinkedHashMap<>();
    for (Class<?> catalog : framework.catalogs) {
      for (Field field : catalog.getFields()) {
        if (!isDefinition(field, framework.execution)) continue;
        Level clash = levels.putIfAbsent(field.getName(), new Level(catalog, value(field)));
        if (clash != null) {
          throw new IllegalStateException(
              "both %s and %s declare %s:%s; rename one, or it cannot be named on the command line"
                  .formatted(
                      clash.catalog().getSimpleName(),
                      catalog.getSimpleName(),
                      framework.prefix(),
                      field.getName()));
        }
      }
    }
    return levels;
  }

  static String prefixes() {
    return Arrays.stream(Framework.values())
        .map(Framework::prefix)
        .collect(Collectors.joining(", "));
  }

  private static Framework framework(String prefix) {
    try {
      return Framework.valueOf(prefix.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "unknown framework: " + prefix + " (expected one of: " + prefixes() + ")");
    }
  }

  private static ExecutionFormula<?> lookup(Framework framework, String level) {
    Map<String, Level> levels = levels(framework);
    Level found = levels.get(level);
    if (found == null) {
      throw new IllegalArgumentException(
          "no such isolation level: "
              + framework.prefix()
              + ":"
              + level
              + "\navailable under "
              + framework.prefix()
              + ": "
              + String.join(", ", levels.keySet()));
    }
    return found.formula();
  }

  /**
   * A catalog class also holds fields that are not levels of its framework — {@link
   * CustomDefinitions}, for one, mixes in {@code HistoryFormula} definitions — so the declared type
   * decides, execution type included: a Biswas formula reached under {@code cerone:} would be
   * wrapped as the wrong kind of constraint and only fail much later, inside the encoder.
   */
  private static boolean isDefinition(Field field, Class<? extends Execution> execution) {
    return Modifier.isStatic(field.getModifiers())
        && ExecutionFormula.class.isAssignableFrom(field.getType())
        && field.getGenericType() instanceof ParameterizedType type
        && type.getActualTypeArguments()[0].equals(execution);
  }

  private static ExecutionFormula<?> value(Field field) {
    try {
      return (ExecutionFormula<?>) field.get(null);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <E extends Execution> ExecutionFormula<E> unchecked(ExecutionFormula<?> formula) {
    return (ExecutionFormula<E>) formula;
  }
}
