package haslab.isolde.biswas;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryExpression;
import haslab.isolde.core.check.candidate.ContextualizedInstance;
import haslab.isolde.core.general.DirectExecutionModule;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.kodkod.Util;
import java.util.ArrayList;
import java.util.List;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class BiswasCandCheckingEncoder
    implements DirectExecutionModule<
        BiswasExecution, ContextualizedInstance, SimpleContext<ContextualizedInstance>> {

  private final List<Relation> coTransReduction = new ArrayList<>();

  public BiswasCandCheckingEncoder(int executions) {
    for (int i = 0; i < executions; i++) {
      coTransReduction.add(Relation.binary("coTransReduction#" + i));
    }
  }

  public BiswasCandCheckingEncoder(Relation coAux) {
    this.coTransReduction.add(coAux);
  }

  @Override
  public List<BiswasExecution> executions(AbstractHistoryK historyEncoding) {
    List<BiswasExecution> executions = new ArrayList<>();
    for (var rel : coTransReduction) {
      executions.add(new BiswasExecution(historyEncoding, rel.closure()));
    }
    return executions;
  }

  @Override
  public int executions() {
    return this.coTransReduction.size();
  }

  @Override
  public SimpleContext<ContextualizedInstance> createContext(ContextualizedInstance input) {
    return new SimpleContext<>(input);
  }

  public BiswasExecution execution(AbstractHistoryK historyEncoding) {
    return executions(historyEncoding).get(0);
  }

  @Override
  public Formula encode(
      Bounds bounds,
      List<ExecutionFormula<BiswasExecution>> formulas,
      SimpleContext<ContextualizedInstance> context,
      AbstractHistoryRel historyEncoding) {
    TupleFactory f = bounds.universe().factory();
    ContextualizedInstance instance = context.val();
    Evaluator ev = new Evaluator(instance.instance());

    TupleSet initialProdNormal =
        convert(instance, f, h -> h.initialTransaction().product(h.normalTxns()), 2);

    TupleSet commitOrderUpperBound =
        Util.irreflexiveBound(
            f, Util.unaryTupleSetToAtoms(ev.evaluate(instance.context().normalTxns())));
    commitOrderUpperBound.addAll(initialProdNormal);

    Formula formula = Formula.TRUE;
    for (int i = 0; i < formulas.size(); i++) {
      Relation lastTxn = Relation.unary("last txn #" + i);
      bounds.bound(coTransReduction.get(i), commitOrderUpperBound);
      bounds.bound(lastTxn, convert(instance, f, AbstractHistoryK::normalTxns, 1));
      Expression commitOrder = coTransReduction.get(i).closure();
      formula =
          formula.and(
              Formula.and(
                  coTransReduction
                      .get(i)
                      .totalOrder(
                          historyEncoding.transactions(),
                          historyEncoding.initialTransaction(),
                          lastTxn),
                  historyEncoding.sessionOrder().union(historyEncoding.binaryWr()).in(commitOrder),
                  formulas.get(i).resolve(executions(historyEncoding).get(i))));
    }

    return formula;
  }

  private TupleSet convert(
      ContextualizedInstance contextualizedInstance,
      TupleFactory tf,
      HistoryExpression expression,
      int arity) {
    Evaluator ev = new Evaluator(contextualizedInstance.instance());
    return Util.convert(ev, contextualizedInstance.context(), expression, tf, arity);
  }
}
