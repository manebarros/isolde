package haslab.isolde.core.general.simple;

import haslab.isolde.core.general.HistoryConstraintProblem;
import haslab.isolde.core.general.HistoryEncoder;
import haslab.isolde.core.general.Input;
import haslab.isolde.core.general.ProblemExtender;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

public class HistoryConstraintProblemS<I extends Input>
    extends HistoryConstraintProblem<I, Void, Void> {

  private static Formula extend(Bounds b, Void helper, List<ProblemExtender<Void>> extenders) {
    Formula formula = Formula.TRUE;
    for (ProblemExtender<Void> ex : extenders) {
      formula = formula.and(ex.extend(null, b));
    }
    return formula;
  }

  public HistoryConstraintProblemS(I input, HistoryEncoder<I, Void> historyEncoder) {
    super(input, historyEncoder, (i, u) -> null, HistoryConstraintProblemS::extend);
  }
}
