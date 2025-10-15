package haslab.isolde.core.general;

public class DirectHistoryConstraintProblem<I extends AtomsContainer>
    extends HistoryConstraintProblem<I, I, Void> {

  public DirectHistoryConstraintProblem(I input, HistoryEncoder<I> historyEncoder) {
    super(
        input,
        historyEncoder,
        (i, u) -> i,
        (f, b, i, history, extenders) -> {
          for (var ex : extenders) {
            f = f.and(ex.encode(b, null, historyEncoder.encoding()));
          }
          return f;
        });
  }
}
