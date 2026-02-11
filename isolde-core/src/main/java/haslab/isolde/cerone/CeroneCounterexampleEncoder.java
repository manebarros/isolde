package haslab.isolde.cerone;

import static haslab.isolde.cerone.definitions.CeroneDefinitions.EXT;
import static haslab.isolde.cerone.definitions.CeroneDefinitions.SESSION;

import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.CounterexampleEncoder;
import haslab.isolde.kodkod.Util;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;

public class CeroneCounterexampleEncoder implements CounterexampleEncoder<CeroneExecution> {

  private static CeroneCounterexampleEncoder instance = null;

  private CeroneCounterexampleEncoder() {}

  public static CeroneCounterexampleEncoder instance() {
    if (instance == null) {
      instance = new CeroneCounterexampleEncoder();
    }
    return instance;
  }

  @Override
  public HistoryFormula guide(
      Instance instance,
      CeroneExecution execution,
      ExecutionFormula<CeroneExecution> formula,
      Bounds bounds) {
    var eval = new Evaluator(instance);
    TupleSet visVal =
        Util.convert(eval, execution, CeroneExecution::vis, bounds.universe().factory(), 2);
    TupleSet arVal =
        Util.convert(eval, execution, CeroneExecution::ar, bounds.universe().factory(), 2);
    Relation cexVisRel = Relation.binary("cex vis");
    Relation cexArRel = Relation.binary("cex ar");
    bounds.boundExactly(cexVisRel, visVal);
    bounds.boundExactly(cexArRel, arVal);
    return h ->
        EXT.and(SESSION).implies(formula).resolve(new CeroneExecution(h, cexVisRel, cexArRel));
  }
}
