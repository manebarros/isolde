package haslab.isolde.cerone;

import static haslab.isolde.cerone.definitions.CeroneDefinitions.EXT;
import static haslab.isolde.cerone.definitions.CeroneDefinitions.SESSION;
import static haslab.isolde.kodkod.Formulas.total;
import static haslab.isolde.kodkod.Formulas.transitive;

import haslab.isolde.core.BindableHistorySchema;
import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.core.HistorySchema;
import haslab.isolde.core.general.ExecutionModule;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.FolSynthesisInput;
import haslab.isolde.core.synth.HistoryAtoms;
import haslab.isolde.kodkod.Translations;
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

  private List<VisAr> relations;

  public CeroneSynthesisModule(int executions) {
    this.relations = new ArrayList<>();
    for (int i = 0; i < executions; i++) {
      relations.add(new VisAr(Relation.binary("vis#" + i), Relation.binary("ar#" + i)));
    }
  }

  @Override
  public List<CeroneExecution> executions(HistorySchema history) {
    List<CeroneExecution> r = new ArrayList<>();
    for (var p : this.relations) {
      r.add(new CeroneExecution(history, p.vis(), p.ar()));
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
      BindableHistorySchema history) {
    return spec.isPresent()
        ? encode(bounds, formulas, context.val(), spec.get(), history)
        : encode(bounds, formulas, context.val(), history);
  }

  public Formula encode(
      Bounds b,
      List<ExecutionFormula<CeroneExecution>> formulas,
      HistoryAtoms historyAtoms,
      HistorySchema history) {
    TupleFactory f = b.universe().factory();
    TupleSet commitOrderTs = Translations.irreflexiveBound(f, historyAtoms.normalTxns());
    TupleSet visAndArLowerBound =
        f.setOf(historyAtoms.initialTxn()).product(f.setOf(historyAtoms.normalTxns().toArray()));
    commitOrderTs.addAll(visAndArLowerBound);
    Formula formula = Formula.TRUE;

    var enc = DirectAbstractHistoryEncoding.INSTANCE;

    for (int i = 0; i < formulas.size(); i++) {
      var rels = relations.get(i);
      Relation vis = rels.vis();
      Relation ar = rels.ar();
      b.bound(vis, visAndArLowerBound, commitOrderTs);
      b.bound(ar, visAndArLowerBound, commitOrderTs);

      var execution = new CeroneExecution(history, vis, ar);
      formula =
          formula
              .and(vis.in(ar))
              .and(transitive(ar))
              .and(total(ar, enc.transactions()))
              .and(EXT.resolve(execution))
              .and(SESSION.resolve(execution))
              .and(formulas.get(i).resolve(execution));
    }
    return formula;
  }

  public Formula encode(
      Bounds b,
      List<ExecutionFormula<CeroneExecution>> formulas,
      HistoryAtoms historyAtoms,
      TupleSet txnTotalOrderTs,
      HistorySchema history) {
    TupleFactory tf = b.universe().factory();
    TupleSet visLb =
        tf.setOf(historyAtoms.initialTxn()).product(tf.setOf(historyAtoms.normalTxns().toArray()));

    var rels = relations.get(0);
    Relation vis = rels.vis();
    Relation ar = rels.ar();
    var execution = new CeroneExecution(history, vis, ar);
    b.boundExactly(ar, txnTotalOrderTs);
    b.bound(vis, visLb, txnTotalOrderTs);

    Formula formula =
        formulas
            .get(0)
            .resolve(execution)
            .and(EXT.resolve(execution))
            .and(SESSION.resolve(execution));

    var enc = DirectAbstractHistoryEncoding.INSTANCE;

    TupleSet commitOrderTs = Translations.irreflexiveBound(tf, historyAtoms.normalTxns());
    commitOrderTs.addAll(visLb);
    for (int i = 1; i < formulas.size(); i++) {
      rels = relations.get(i);
      vis = rels.vis();
      ar = rels.ar();
      execution = new CeroneExecution(history, vis, ar);
      b.bound(vis, visLb, commitOrderTs);
      b.bound(ar, visLb, commitOrderTs);
      formula =
          formula
              .and(vis.in(ar))
              .and(transitive(ar))
              .and(total(ar, enc.transactions()))
              .and(EXT.resolve(execution))
              .and(SESSION.resolve(execution))
              .and(formulas.get(i).resolve(execution));
    }
    return formula;
  }
}
