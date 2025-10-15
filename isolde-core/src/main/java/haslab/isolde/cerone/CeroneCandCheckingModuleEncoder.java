package haslab.isolde.cerone;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryExpression;
import haslab.isolde.core.check.candidate.ContextualizedInstance;
import haslab.isolde.core.general.DirectExecutionModule;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.kodkod.Util;
import haslab.isolde.util.Pair;
import java.util.ArrayList;
import java.util.List;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class CeroneCandCheckingModuleEncoder
    implements DirectExecutionModule<
        CeroneExecution, ContextualizedInstance, SimpleContext<ContextualizedInstance>> {

  private List<Pair<Relation>> orderings;

  public CeroneCandCheckingModuleEncoder(Relation vis, Relation arTransReduction) {
    this.orderings = new ArrayList<>();
    this.orderings.add(new Pair<>(vis, arTransReduction));
  }

  public CeroneCandCheckingModuleEncoder(int executions) {
    this.orderings = new ArrayList<>();
    for (int i = 0; i < executions; i++) {
      Relation vis = Relation.binary("vis #" + i);
      Relation arTransReduction = Relation.binary("ar's transitive reduction #" + i);
      orderings.add(new Pair<>(vis, arTransReduction));
    }
  }

  @Override
  public List<CeroneExecution> executions(AbstractHistoryK historyEncoding) {
    List<CeroneExecution> r = new ArrayList<>();
    for (var p : orderings) {
      r.add(new CeroneExecution(historyEncoding, p.fst(), p.snd().closure()));
    }
    return r;
  }

  @Override
  public int executions() {
    return this.orderings.size();
  }

  @Override
  public SimpleContext<ContextualizedInstance> createContext(ContextualizedInstance input) {
    return new SimpleContext<>(input);
  }

  @Override
  public Formula encode(
      Bounds b,
      List<ExecutionFormula<CeroneExecution>> formulas,
      SimpleContext<ContextualizedInstance> context,
      AbstractHistoryRel historyEncoding) {

    var contextualizedInstance = context.val();

    TupleFactory tf = b.universe().factory();
    Evaluator ev = new Evaluator(contextualizedInstance.instance());

    TupleSet visLowerBound =
        convert(contextualizedInstance, tf, h -> h.initialTransaction().product(h.normalTxns()), 2);

    TupleSet visUpperBound =
        Util.irreflexiveBound(
            tf,
            Util.unaryTupleSetToAtoms(ev.evaluate(contextualizedInstance.context().normalTxns())));
    visUpperBound.addAll(visLowerBound);

    Formula formula = Formula.TRUE;

    for (int i = 0; i < formulas.size(); i++) {
      Relation lastTxn = Relation.unary("Last Txn #" + i);
      b.bound(orderings.get(i).fst(), visLowerBound, visUpperBound);
      b.bound(orderings.get(i).snd(), visUpperBound);
      b.bound(lastTxn, convert(contextualizedInstance, tf, AbstractHistoryK::normalTxns, 1));
      Expression vis = orderings.get(i).fst();
      Relation arTransReduction = orderings.get(i).snd();
      Expression ar = arTransReduction.closure();

      formula =
          formula.and(
              Formula.and(
                  vis.in(ar),
                  historyEncoding.sessionOrder().in(ar),
                  arTransReduction.totalOrder(
                      historyEncoding.transactions(),
                      historyEncoding.initialTransaction(),
                      lastTxn),
                  formulas.get(i).resolve(executions(historyEncoding).get(i))));
    }

    return formula;
  }

  private TupleSet convert(
      ContextualizedInstance instance, TupleFactory tf, HistoryExpression expression, int arity) {
    Evaluator ev = new Evaluator(instance.instance());
    return Util.convert(ev, instance.context(), expression, tf, arity);
  }
}
