package haslab.isolde.biswas;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.AbstractHistoryRel;
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

public class BiswasCandCheckingEncoder
    implements ExecutionConstraintsEncoderS<ContextualizedInstance, BiswasExecution> {
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

  public BiswasExecution execution(AbstractHistoryK historyEncoding) {
    return executions(historyEncoding).get(0);
  }

  @Override
  public ProblemExtenderS encode(
      ContextualizedInstance instance,
      AbstractHistoryRel historyEncoding,
      List<ExecutionFormula<BiswasExecution>> formulas) {

    var encoder = this;

    return new ProblemExtenderS() {

      private Evaluator ev = new Evaluator(instance.instance());

      private TupleSet convert(TupleFactory tf, HistoryExpression expression, int arity) {
        return Util.convert(this.ev, instance.context(), expression, tf, arity);
      }

      @Override
      public Collection<Object> extraAtoms() {
        return new ArrayList<>();
      }

      @Override
      public Formula extend(Bounds b) {
        TupleFactory f = b.universe().factory();
        Evaluator ev = new Evaluator(instance.instance());

        TupleSet initialProdNormal =
            convert(f, h -> h.initialTransaction().product(h.normalTxns()), 2);

        TupleSet commitOrderUpperBound =
            Util.irreflexiveBound(
                f, Util.unaryTupleSetToAtoms(ev.evaluate(instance.context().normalTxns())));
        commitOrderUpperBound.addAll(initialProdNormal);

        Formula formula = Formula.TRUE;
        for (int i = 0; i < formulas.size(); i++) {
          Relation lastTxn = Relation.unary("last txn #" + i);
          b.bound(coTransReduction.get(i), commitOrderUpperBound);
          b.bound(lastTxn, convert(f, AbstractHistoryK::normalTxns, 1));
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
                      historyEncoding
                          .sessionOrder()
                          .union(historyEncoding.binaryWr())
                          .in(commitOrder),
                      formulas.get(i).resolve(encoder.executions(historyEncoding).get(i))));
        }

        return formula;
      }
    };
  }
}
