package haslab.isolde.cerone;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.check.external.CheckingIntermediateRepresentation;
import haslab.isolde.core.general.simple.ExecutionConstraintsEncoderS;
import haslab.isolde.core.general.simple.ProblemExtenderS;
import haslab.isolde.kodkod.KodkodUtil;
import haslab.isolde.kodkod.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class CeroneHistCheckingModuleEncoder
    implements ExecutionConstraintsEncoderS<CheckingIntermediateRepresentation, CeroneExecution> {
  private List<RelationPair> orderings;

  public static record RelationPair(Relation fst, Relation snd) {}

  public CeroneHistCheckingModuleEncoder(Relation vis, Relation arTransReduction) {
    this.orderings = new ArrayList<>();
    this.orderings.add(new RelationPair(vis, arTransReduction));
  }

  public CeroneHistCheckingModuleEncoder(int executions) {
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
      CheckingIntermediateRepresentation intermediateRepresentation,
      AbstractHistoryRel historyEncoding,
      List<ExecutionFormula<CeroneExecution>> formulas) {

    var encoder = this;

    return new ProblemExtenderS() {

      @Override
      public Collection<Object> extraAtoms() {
        return new ArrayList<>();
      }

      @Override
      public Formula extend(Bounds b) {
        TupleFactory tf = b.universe().factory();
        TupleSet visLowerBound =
            tf.setOf(intermediateRepresentation.getInitialTxnAtom())
                .product(KodkodUtil.asTupleSet(tf, intermediateRepresentation.normalTxnAtoms()));
        TupleSet visUpperBound =
            Util.irreflexiveBound(tf, intermediateRepresentation.normalTxnAtoms());
        visUpperBound.addAll(visLowerBound);

        Formula formula = Formula.TRUE;
        for (int i = 0; i < formulas.size(); i++) {
          Relation lastTxn = Relation.unary("Last Txn #" + i);
          b.bound(orderings.get(i).fst(), visLowerBound, visUpperBound);
          b.bound(orderings.get(i).snd(), visUpperBound);
          b.bound(lastTxn, KodkodUtil.asTupleSet(tf, intermediateRepresentation.normalTxnAtoms()));
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
