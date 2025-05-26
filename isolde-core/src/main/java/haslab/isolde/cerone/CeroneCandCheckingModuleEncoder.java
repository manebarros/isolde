package haslab.isolde.cerone;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryExpression;
import haslab.isolde.core.check.candidate.ContextualizedInstance;
import haslab.isolde.core.general.simple.ExecutionConstraintsEncoderS;
import haslab.isolde.core.general.simple.ProblemExtenderS;
import haslab.isolde.kodkod.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class CeroneCandCheckingModuleEncoder
    implements ExecutionConstraintsEncoderS<ContextualizedInstance, CeroneExecution> {
  private List<RelationPair> orderings;

  public static record RelationPair(Relation fst, Relation snd) {}

  public CeroneCandCheckingModuleEncoder(Relation vis, Relation arTransReduction) {
    this.orderings = new ArrayList<>();
    this.orderings.add(new RelationPair(vis, arTransReduction));
  }

  public CeroneCandCheckingModuleEncoder(int executions) {
    this.orderings = new ArrayList<>();
    for (int i = 0; i < executions; i++) {
      Relation vis = Relation.binary("vis #" + i);
      Relation arTransReduction = Relation.binary("ar's transitive reduction #" + i);
      orderings.add(new RelationPair(vis, arTransReduction));
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
  public ProblemExtenderS encode(
      ContextualizedInstance contextualizedInstance,
      AbstractHistoryK historyEncoding,
      List<ExecutionFormula<CeroneExecution>> formulas) {

    var encoder = this;
    var instance = contextualizedInstance.instance();
    var context = contextualizedInstance.context();

    return new ProblemExtenderS() {

      private Evaluator ev = new Evaluator(instance);

      private TupleSet convert(TupleFactory tf, HistoryExpression expression, int arity) {
        return Util.convert(this.ev, context, expression, tf, arity);
      }

      @Override
      public Collection<Object> extraAtoms() {
        return new ArrayList<>();
      }

      @Override
      public Formula extend(Bounds b) {
        TupleFactory tf = b.universe().factory();
        Evaluator ev = new Evaluator(instance);

        TupleSet visLowerBound =
            convert(tf, h -> h.initialTransaction().product(h.normalTxns()), 2);

        TupleSet visUpperBound =
            Util.irreflexiveBound(tf, Util.unaryTupleSetToAtoms(ev.evaluate(context.normalTxns())));
        visUpperBound.addAll(visLowerBound);

        Formula formula = Formula.TRUE;

        for (int i = 0; i < formulas.size(); i++) {
          Relation lastTxn = Relation.unary("Last Txn #" + i);
          b.bound(orderings.get(i).fst(), visLowerBound, visUpperBound);
          b.bound(orderings.get(i).snd(), visUpperBound);
          b.bound(lastTxn, convert(tf, AbstractHistoryK::normalTxns, 1));
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
                      formulas.get(i).resolve(encoder.executions(historyEncoding).get(i))));
        }

        return formula;
      }
    };
  }
}
