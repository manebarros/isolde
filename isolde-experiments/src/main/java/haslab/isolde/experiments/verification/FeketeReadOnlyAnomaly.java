package haslab.isolde.experiments.verification;

import static haslab.isolde.history.Operation.readOf;
import static haslab.isolde.history.Operation.writeOf;

import haslab.isolde.Synthesizer;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.BiswasHistCheckingEncoder;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.CeroneHistCheckingModuleEncoder;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.HistoryExpression;
import haslab.isolde.core.HistoryFormula;
import haslab.isolde.core.cegis.SynthesisSpec;
import haslab.isolde.core.check.external.DefaultHistoryCheckingEncoder;
import haslab.isolde.core.check.external.HistCheckEncoder;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.History;
import haslab.isolde.history.Session;
import haslab.isolde.history.Transaction;
import haslab.isolde.kodkod.KodkodProblem;
import java.util.Arrays;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;

public final class FeketeReadOnlyAnomaly {
  public static final HistoryExpression updateTransactions =
      h -> h.finalWrites().join(h.values()).join(h.keys());

  public static final Formula updateSer(BiswasExecution e) {
    return AxiomaticDefinitions.Ser.resolve(
        new BiswasExecution(
            e.history().subHistory(updateTransactions.resolve(e.history())), e.co()));
  }

  public static final Formula updateSer(CeroneExecution e) {
    return CeroneDefinitions.SER.resolve(
        new CeroneExecution(
            e.history().subHistory(updateTransactions.resolve(e.history())), e.vis(), e.ar()));
  }

  public static final Formula updateSerAlt(CeroneExecution e) {
    Variable t, s;
    t = Variable.unary("t");
    s = Variable.unary("s");
    return Formula.and(
            t.eq(s).not(), e.history().isUpdateTransaction(t), e.history().isUpdateTransaction(s))
        .implies(t.product(s).in(e.vis()).or(s.product(t).in(e.vis())))
        .forAll(t.oneOf(e.history().transactions()).and(s.oneOf(e.history().transactions())))
        .and(CeroneDefinitions.EXT.resolve(e));
  }

  public static final Expression updateTxnCo(BiswasExecution e) {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    return t.product(s)
        .in(e.co())
        .and(t.join(e.history().finalWrites()).some())
        .and(s.join(e.history().finalWrites()).some())
        .comprehension(
            t.oneOf(e.history().transactions()).and(s.oneOf(e.history().transactions())));
  }

  public static final void generateReadOnlyAnomaly() {
    Scope scope = new Scope(3, 2, 3, 3);
    SynthesisSpec<BiswasExecution> spec =
        new SynthesisSpec<>(
            Arrays.asList(AxiomaticDefinitions.Snapshot, FeketeReadOnlyAnomaly::updateSer),
            AxiomaticDefinitions.Ser.not());
    HistoryFormula oneTransactionPerSession =
        h -> h.initialTransaction().product(h.normalTxns()).eq(h.sessionOrder());
    Synthesizer synth = new Synthesizer(scope, oneTransactionPerSession);
    synth.registerBiswas(spec);
    System.out.println(synth.synthesize().get());
  }

  public static final void generateReadOnlyAnomalyCeroneAlt() {
    Scope scope = new Scope(3, 2, 2, 2);
    SynthesisSpec<CeroneExecution> spec =
        new SynthesisSpec<>(
            Arrays.asList(CeroneDefinitions.SI, FeketeReadOnlyAnomaly::updateSer),
            CeroneDefinitions.SER.not());
    //    HistoryFormula oneTransactionPerSession =
    //        h -> h.initialTransaction().product(h.normalTxns()).eq(h.sessionOrder());
    //    Synthesizer synth = new Synthesizer(scope, oneTransactionPerSession);
    Synthesizer synth = new Synthesizer(scope);
    synth.registerCerone(spec);
    System.out.println(synth.synthesize().get());
  }

  public static final History readOnlyAnomaly =
      new History(
          new Session(new Transaction(1, Arrays.asList(readOf(0, 0), readOf(1, 0), writeOf(0, 1)))),
          new Session(new Transaction(3, Arrays.asList(readOf(0, 0), readOf(1, 1)))),
          new Session(new Transaction(2, Arrays.asList(readOf(1, 0), writeOf(1, 1)))));

  public static final HistCheckEncoder<BiswasExecution> encoder() {
    return new HistCheckEncoder<>(
        DefaultHistoryCheckingEncoder.instance(), BiswasHistCheckingEncoder::new);
  }

  public static final HistCheckEncoder<CeroneExecution> ceroneEncoder() {
    return new HistCheckEncoder<>(
        DefaultHistoryCheckingEncoder.instance(), new CeroneHistCheckingModuleEncoder(1));
  }

  public static final void checkReadOnlyAnomally() {
    KodkodProblem p = encoder().encode(readOnlyAnomaly, AxiomaticDefinitions.Snapshot);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    System.out.println("allowed under SI: " + sol.sat());
    System.out.println(sol.instance());

    p = encoder().encode(readOnlyAnomaly, FeketeReadOnlyAnomaly::updateSer);
    sol = new Solver().solve(p.formula(), p.bounds());
    System.out.println("allowed under update ser: " + sol.sat());
    System.out.println(sol.instance());

    p = encoder().encode(readOnlyAnomaly, AxiomaticDefinitions.Ser);
    sol = new Solver().solve(p.formula(), p.bounds());
    System.out.println("allowed under ser: " + sol.sat());
  }
}
