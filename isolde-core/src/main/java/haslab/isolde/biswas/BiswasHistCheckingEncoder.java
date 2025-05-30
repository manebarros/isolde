package haslab.isolde.biswas;

import static haslab.isolde.kodkod.KodkodUtil.asTupleSet;

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

public class BiswasHistCheckingEncoder
    implements ExecutionConstraintsEncoderS<CheckingIntermediateRepresentation, BiswasExecution> {
  private final List<Relation> coTransReduction = new ArrayList<>();

  public BiswasHistCheckingEncoder(int executions) {
    for (int i = 0; i < executions; i++) {
      coTransReduction.add(Relation.binary("coTransReduction#" + i));
    }
  }

  public BiswasHistCheckingEncoder(Relation coAux) {
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
      CheckingIntermediateRepresentation intermediateRepresentation,
      AbstractHistoryRel historyEncoding,
      List<ExecutionFormula<BiswasExecution>> formulas) {

    List<Relation> rels = this.coTransReduction;

    return new ProblemExtenderS() {

      @Override
      public Collection<Object> extraAtoms() {
        return new ArrayList<>();
      }

      @Override
      public Formula extend(Bounds b) {
        TupleFactory tf = b.universe().factory();

        TupleSet initProdNormal =
            tf.setOf(intermediateRepresentation.getInitialTxnAtom())
                .product(KodkodUtil.asTupleSet(tf, intermediateRepresentation.normalTxnAtoms()));
        TupleSet coUpperBound =
            Util.irreflexiveBound(tf, intermediateRepresentation.normalTxnAtoms());
        coUpperBound.addAll(initProdNormal);

        Formula formula = Formula.TRUE;
        for (int i = 0; i < formulas.size(); i++) {
          Relation coTransReduction = rels.get(i);
          Expression co = coTransReduction.closure();
          Relation lastTxn = Relation.unary("Last Txn #" + i);
          b.bound(coTransReduction, coUpperBound);
          b.bound(lastTxn, asTupleSet(tf, intermediateRepresentation.normalTxnAtoms()));
          formula =
              formula.and(
                  Formula.and(
                      coTransReduction.totalOrder(
                          historyEncoding.transactions(),
                          historyEncoding.initialTransaction(),
                          lastTxn),
                      historyEncoding.sessionOrder().union(historyEncoding.binaryWr()).in(co),
                      formulas.get(i).resolve(executions(historyEncoding).get(i))));
        }

        return formula;
      }
    };
  }
}
