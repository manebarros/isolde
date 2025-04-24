package com.github.manebarros.biswas;

import static com.github.manebarros.kodkod.KodkodUtil.asTupleSet;

import com.github.manebarros.core.AbstractHistoryK;
import com.github.manebarros.core.ExecutionFormula;
import com.github.manebarros.core.HistoryExpression;
import com.github.manebarros.core.check.CheckingProblemExtender;
import com.github.manebarros.core.check.candidate.CandCheckModuleEncoder;
import com.github.manebarros.core.check.external.CheckingIntermediateRepresentation;
import com.github.manebarros.core.check.external.HistCheckModuleEncoder;
import com.github.manebarros.kodkod.KodkodUtil;
import com.github.manebarros.kodkod.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class BiswasCheckingEncoder
    implements CandCheckModuleEncoder<BiswasExecution>, HistCheckModuleEncoder<BiswasExecution> {
  private final List<Relation> coTransReduction = new ArrayList<>();

  public BiswasCheckingEncoder(int executions) {
    for (int i = 0; i < executions; i++) {
      coTransReduction.add(Relation.binary("coTransReduction#" + i));
    }
  }

  public BiswasCheckingEncoder(Relation coAux) {
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
  public CheckingProblemExtender encode(
      Instance instance,
      AbstractHistoryK context,
      AbstractHistoryK historyEncoding,
      List<ExecutionFormula<BiswasExecution>> formulas) {

    var encoder = this;

    return new CheckingProblemExtender() {

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
        TupleFactory f = b.universe().factory();
        Evaluator ev = new Evaluator(instance);

        TupleSet initialProdNormal =
            convert(f, h -> h.initialTransaction().product(h.normalTxns()), 2);

        TupleSet commitOrderUpperBound =
            Util.irreflexiveBound(f, Util.unaryTupleSetToAtoms(ev.evaluate(context.normalTxns())));
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

  @Override
  public CheckingProblemExtender encode(
      CheckingIntermediateRepresentation intermediateRepresentation,
      AbstractHistoryK historyEncoding,
      List<ExecutionFormula<BiswasExecution>> formulas) {

    List<Relation> rels = this.coTransReduction;

    return new CheckingProblemExtender() {

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
