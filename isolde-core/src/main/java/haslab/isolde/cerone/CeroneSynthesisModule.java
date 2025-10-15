package haslab.isolde.cerone;

import static haslab.isolde.kodkod.KodkodUtil.total;
import static haslab.isolde.kodkod.KodkodUtil.transitive;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.general.ExecutionModule;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.kodkod.Util;
import haslab.isolde.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public class CeroneSynthesisModule
    implements ExecutionModule<
        CeroneExecution, FolSynthesisInput, Optional<TupleSet>, SimpleContext<HistoryAtoms>> {

  private List<Pair<Relation>> relations;

  private Relation vis(Pair<Relation> relations) {
    return relations.fst();
  }

  private Relation ar(Pair<Relation> relations) {
    return relations.snd();
  }

  public CeroneSynthesisModule(int executions) {
    this.relations = new ArrayList<>();
    for (int i = 0; i < executions; i++) {
      relations.add(new Pair<>(Relation.binary("vis#" + i), Relation.binary("ar#" + i)));
    }
  }

  @Override
  public List<CeroneExecution> executions(AbstractHistoryK history) {
    List<CeroneExecution> r = new ArrayList<>();
    for (var p : this.relations) {
      r.add(new CeroneExecution(history, vis(p), ar(p)));
    }
    return r;
  }

  @Override
  public int executions() {
    return this.relations.size();
  }

  @Override
  public SimpleContext<HistoryAtoms> createContext(FolSynthesisInput input) {
    return new SimpleContext<>(input.historyAtoms());
  }

  @Override
  public Formula encode(
      Bounds bounds,
      List<ExecutionFormula<CeroneExecution>> formulas,
      SimpleContext<HistoryAtoms> context,
      Optional<TupleSet> spec,
      AbstractHistoryRel history) {
    return spec.isPresent()
        ? encode(bounds, formulas, context.val(), spec.get(), history)
        : encode(bounds, formulas, context.val(), history);
  }

  public Formula encode(
      Bounds b,
      List<ExecutionFormula<CeroneExecution>> formulas,
      HistoryAtoms historyAtoms,
      AbstractHistoryK history) {
    TupleFactory f = b.universe().factory();
    TupleSet commitOrderTs = Util.irreflexiveBound(f, historyAtoms.normalTxns());
    TupleSet visAndArLowerBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));
    commitOrderTs.addAll(visAndArLowerBound);
    Formula formula = Formula.TRUE;

    var enc = DirectAbstractHistoryEncoding.instance();

    for (int i = 0; i < formulas.size(); i++) {
      var execution = relations.get(i);
      Relation vis = vis(execution);
      Relation ar = ar(execution);
      b.bound(vis, visAndArLowerBound, commitOrderTs);
      b.bound(ar, visAndArLowerBound, commitOrderTs);
      formula =
          formula
              .and(vis.in(ar))
              .and(transitive(ar))
              .and(enc.sessionOrder().in(ar))
              .and(total(ar, enc.transactions()))
              .and(formulas.get(i).resolve(new CeroneExecution(history, vis, ar)));
    }
    return formula;
  }

  public Formula encode(
      Bounds b,
      List<ExecutionFormula<CeroneExecution>> formulas,
      HistoryAtoms historyAtoms,
      TupleSet txnTotalOrderTs,
      AbstractHistoryK history) {
    TupleFactory tf = b.universe().factory();
    TupleSet visLb =
        tf.setOf(historyAtoms.initialTxn()).product(tf.setOf(historyAtoms.normalTxns().toArray()));

    b.boundExactly(ar(relations.get(0)), txnTotalOrderTs);
    b.bound(vis(relations.get(0)), visLb, txnTotalOrderTs);
    Formula formula =
        formulas
            .get(0)
            .resolve(new CeroneExecution(history, vis(relations.get(0)), ar(relations.get(0))));

    var enc = DirectAbstractHistoryEncoding.instance();

    TupleSet commitOrderTs = Util.irreflexiveBound(tf, historyAtoms.normalTxns());
    commitOrderTs.addAll(visLb);
    for (int i = 1; i < formulas.size(); i++) {
      var execution = relations.get(i);
      Relation vis = vis(execution);
      Relation ar = ar(execution);
      b.bound(vis, visLb, commitOrderTs);
      b.bound(ar, visLb, commitOrderTs);
      formula =
          formula
              .and(vis.in(ar))
              .and(transitive(ar))
              .and(enc.sessionOrder().in(ar))
              .and(total(ar, enc.transactions()))
              .and(formulas.get(i).resolve(new CeroneExecution(history, vis, ar)));
    }
    return formula;
  }
}
