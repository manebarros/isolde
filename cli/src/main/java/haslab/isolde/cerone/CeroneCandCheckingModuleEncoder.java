package haslab.isolde.cerone;

import static haslab.isolde.cerone.definitions.CeroneDefinitions.EXT;
import static haslab.isolde.cerone.definitions.CeroneDefinitions.SESSION;

import haslab.isolde.core.BindableHistorySchema;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistoryExpression;
import haslab.isolde.core.HistorySchema;
import haslab.isolde.core.check.candidate.Candidate;
import haslab.isolde.core.general.DirectExecutionModule;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.kodkod.Translations;
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
    implements DirectExecutionModule<CeroneExecution, Candidate, SimpleContext<Candidate>> {

  private List<VisArTransReduction> orderings;

  public CeroneCandCheckingModuleEncoder(Relation vis, Relation arTransReduction) {
    this.orderings = new ArrayList<>();
    this.orderings.add(new VisArTransReduction(vis, arTransReduction));
  }

  public CeroneCandCheckingModuleEncoder(int executions) {
    this.orderings = new ArrayList<>();
    for (int i = 0; i < executions; i++) {
      Relation vis = Relation.binary("vis #" + i);
      Relation arTransReduction = Relation.binary("ar's transitive reduction #" + i);
      orderings.add(new VisArTransReduction(vis, arTransReduction));
    }
  }

  @Override
  public List<CeroneExecution> executions(HistorySchema historyEncoding) {
    List<CeroneExecution> r = new ArrayList<>();
    for (var p : orderings) {
      r.add(new CeroneExecution(historyEncoding, p.vis(), p.arTransReduction().closure()));
    }
    return r;
  }

  @Override
  public int executions() {
    return this.orderings.size();
  }

  @Override
  public SimpleContext<Candidate> createContext(Candidate input) {
    return new SimpleContext<>(input);
  }

  @Override
  public Formula encode(
      Bounds b,
      List<ExecutionFormula<CeroneExecution>> formulas,
      SimpleContext<Candidate> context,
      BindableHistorySchema historyEncoding) {

    var contextualizedInstance = context.val();

    TupleFactory tf = b.universe().factory();
    Evaluator ev = new Evaluator(contextualizedInstance.instance());

    TupleSet visLowerBound =
        convert(contextualizedInstance, tf, h -> h.initialTransaction().product(h.normalTxns()), 2);

    TupleSet visUpperBound =
        Translations.irreflexiveBound(
            tf,
            Translations.unaryTupleSetToAtoms(
                ev.evaluate(contextualizedInstance.context().normalTxns())));
    visUpperBound.addAll(visLowerBound);

    Formula formula = Formula.TRUE;

    for (int i = 0; i < formulas.size(); i++) {
      Relation lastTxn = Relation.unary("Last Txn #" + i);
      var ordering = orderings.get(i);
      b.bound(ordering.vis(), visLowerBound, visUpperBound);
      b.bound(ordering.arTransReduction(), visUpperBound);
      b.bound(lastTxn, convert(contextualizedInstance, tf, HistorySchema::normalTxns, 1));
      Expression vis = ordering.vis();
      Relation arTransReduction = ordering.arTransReduction();
      Expression ar = arTransReduction.closure();
      var execution = new CeroneExecution(historyEncoding, vis, ar);

      formula =
          formula.and(
              Formula.and(
                  vis.in(ar),
                  arTransReduction.totalOrder(
                      historyEncoding.transactions(),
                      historyEncoding.initialTransaction(),
                      lastTxn),
                  EXT.resolve(execution),
                  SESSION.resolve(execution),
                  formulas.get(i).resolve(execution)));
    }

    return formula;
  }

  private TupleSet convert(
      Candidate instance, TupleFactory tf, HistoryExpression expression, int arity) {
    Evaluator ev = new Evaluator(instance.instance());
    return Translations.convert(ev, instance.context(), expression, tf, arity);
  }
}
