package haslab.isolde.core.general;

import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionFormula;
import java.util.Collection;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

@FunctionalInterface
public interface ExecutionModule<I extends Input, T, E extends Execution> {

  ContextualizedExtender<E, T> encode(
      I input, AbstractHistoryRel historyEncoding, List<ExecutionFormula<E>> formulas);

  public static <I extends Input, T, E extends Execution>
      ExecutionModule<I, T, E> fromExecutionConstraintsEncoderConstructor(
          ExecutionConstraintsEncoderConstructor<I, T, E> encoderConstructor) {
    return (i, historyEncoding, formulas) -> {
      ExecutionConstraintsEncoder<I, T, E> encoder = encoderConstructor.generate(formulas.size());
      ProblemExtender<T> extender = encoder.encode(i, historyEncoding, formulas);

      return new ContextualizedExtender<E, T>() {
        @Override
        public Collection<Object> extraAtoms() {
          return extender.extraAtoms();
        }

        @Override
        public Formula extend(T extra, Bounds b) {
          return extender.extend(extra, b);
        }

        @Override
        public List<E> executions() {
          return encoder.executions(historyEncoding);
        }
      };
    };
  }
}
