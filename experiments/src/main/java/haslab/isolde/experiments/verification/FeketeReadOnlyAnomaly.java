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
import haslab.isolde.core.check.external.CheckingIntermediateRepresentation;
import haslab.isolde.core.check.external.HistCheckEncoder;
import haslab.isolde.core.general.DirectExecutionModuleConstructor;
import haslab.isolde.core.general.SimpleContext;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.History;
import haslab.isolde.history.Session;
import haslab.isolde.history.Transaction;
import haslab.isolde.kodkod.KodkodProblem;
import java.util.Arrays;
import kodkod.ast.Formula;
import kodkod.ast.Variable;
import kodkod.engine.Evaluator;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.satlab.SATFactory;

public final class FeketeReadOnlyAnomaly {
  public static final HistoryExpression updateTransactions =
      h -> h.finalWrites().join(h.values()).join(h.keys());

  public static final Formula updateSer(BiswasExecution e) {
    return AxiomaticDefinitions.Serializability(
        new BiswasExecution(e.history().subHistory(updateTransactions), e.co()));
  }

  public static final Formula updateSerExplicit(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(t1.eq(t2).not(), e.history().wr(t1, x, t3), t2.product(t3).in(e.co()))
        .implies(t1.in(t2.join(e.co())))
        .forAll(
            x.oneOf(e.history().keys())
                .and(
                    t1.oneOf(e.history().txnThatWriteToAnyOf(x))
                        .and(
                            t2.oneOf(e.history().txnThatWriteToAnyOf(x))
                                .and(t3.oneOf(e.history().updateTransactions())))));
  }

  public static final Formula updateSer(CeroneExecution e) {
    return CeroneDefinitions.EXT
        .resolve(e)
        .and(
            CeroneDefinitions.TOTAL_VIS.resolve(
                new CeroneExecution(e.history().subHistory(updateTransactions), e.vis(), e.ar())));
  }

  public static final Formula updateSerCeroneWrong(CeroneExecution e) {
    return CeroneDefinitions.Ser.resolve(
        new CeroneExecution(e.history().subHistory(updateTransactions), e.vis(), e.ar()));
  }

  public static final HistoryFormula oneTransactionPerSession =
      h -> h.initialTransaction().product(h.normalTxns()).eq(h.sessionOrder());

  // Generate anomaly using Biswas framework and print it.
  public static final void generateAnomalyBiswas() {
    Scope scope = new Scope.Builder(3).obj(2).val(2).build();
    SynthesisSpec<BiswasExecution> spec =
        new SynthesisSpec<>(
            Arrays.asList(AxiomaticDefinitions.Snapshot, FeketeReadOnlyAnomaly::updateSer),
            AxiomaticDefinitions.Ser.not());

    Synthesizer synth = new Synthesizer(scope);

    synth.registerBiswas(spec);
    var result = synth.synthesize(SATFactory.MiniSat);
    System.out.printf("candidates: %d\n", result.candidates());
    System.out.println(result);
    System.out.println(
        new Evaluator(result.cegisResult().getSolution().get())
            .evaluate(synth.getCegisSynthesizer().historyEncoding().sessionOrder()));

    System.out.println(
        new Evaluator(result.cegisResult().getSolution().get())
            .evaluate(
                oneTransactionPerSession.resolve(synth.getCegisSynthesizer().historyEncoding())));
  }

  // DEBUG
  public static final void debug() {
    Scope scope = new Scope.Builder(3).obj(2).val(2).build();
    SynthesisSpec<BiswasExecution> spec =
        new SynthesisSpec<>(
            Arrays.asList(FeketeReadOnlyAnomaly::updateSer, AxiomaticDefinitions.Snapshot),
            AxiomaticDefinitions.Ser.not());
    HistoryFormula oneTransactionPerSession =
        h -> h.initialTransaction().product(h.normalTxns()).eq(h.sessionOrder());

    Synthesizer synth = new Synthesizer(scope, oneTransactionPerSession);

    synth.registerBiswas(spec);
    var result = synth.debug(SATFactory.MiniSat, readOnlyAnomaly);
    System.out.println(result);
  }

  public static final void generateAnomalyCerone() {
    Scope scope = new Scope.Builder(3).obj(2).val(2).build();
    SynthesisSpec<CeroneExecution> spec =
        new SynthesisSpec<>(
            Arrays.asList(CeroneDefinitions.SI, FeketeReadOnlyAnomaly::updateSer),
            CeroneDefinitions.Ser.not());
    HistoryFormula oneTransactionPerSession =
        h -> h.initialTransaction().product(h.normalTxns()).eq(h.sessionOrder());
    Synthesizer synth = new Synthesizer(scope, oneTransactionPerSession);
    synth.registerCerone(spec);
    System.out.println(synth.synthesize().history());
  }

  // An example of the Fekete anomaly
  public static final History readOnlyAnomaly =
      new History(
          new Session(new Transaction(1, Arrays.asList(readOf(0, 0), readOf(1, 0), writeOf(0, 1)))),
          new Session(new Transaction(3, Arrays.asList(readOf(0, 0), readOf(1, 1)))),
          new Session(new Transaction(2, Arrays.asList(readOf(1, 0), writeOf(1, 1)))));

  public static final HistCheckEncoder<BiswasExecution> encoder() {
    DirectExecutionModuleConstructor<
            BiswasExecution,
            CheckingIntermediateRepresentation,
            SimpleContext<CheckingIntermediateRepresentation>>
        constructor = BiswasHistCheckingEncoder::new;
    return new HistCheckEncoder<>(constructor);
  }

  public static final HistCheckEncoder<CeroneExecution> ceroneEncoder() {
    DirectExecutionModuleConstructor<
            CeroneExecution,
            CheckingIntermediateRepresentation,
            SimpleContext<CheckingIntermediateRepresentation>>
        constructor = CeroneHistCheckingModuleEncoder::new;
    return new HistCheckEncoder<>(constructor);
  }

  public static final void checkReadOnlyAnomallyBiswas() {
    KodkodProblem p = encoder().encode(readOnlyAnomaly, AxiomaticDefinitions.Snapshot);
    Solution sol = new Solver().solve(p.formula(), p.bounds());
    System.out.println("allowed under Biswas SI: " + sol.sat());
    System.out.println(sol.instance());

    p = encoder().encode(readOnlyAnomaly, FeketeReadOnlyAnomaly::updateSer);
    sol = new Solver().solve(p.formula(), p.bounds());
    System.out.println("allowed under Biswas UpdateSer: " + sol.sat());
    System.out.println(sol.instance());

    p = encoder().encode(readOnlyAnomaly, AxiomaticDefinitions.Ser);
    sol = new Solver().solve(p.formula(), p.bounds());
    System.out.println("allowed under Biswas Ser: " + sol.sat());
  }

  public static final void checkReadOnlyAnomallyCerone() {
    KodkodProblem p = ceroneEncoder().encode(readOnlyAnomaly, CeroneDefinitions.SI);
    Solution sol = p.solve(new Solver());
    System.out.println("allowed under Cerone's SI: " + sol.sat());
    System.out.println(sol.instance());

    p = ceroneEncoder().encode(readOnlyAnomaly, FeketeReadOnlyAnomaly::updateSer);
    sol = p.solve(new Solver());
    System.out.println("allowed under Cerone's UpdateSer: " + sol.sat());
    System.out.println(sol.instance());

    p = ceroneEncoder().encode(readOnlyAnomaly, CeroneDefinitions.Ser);
    sol = p.solve(new Solver());
    System.out.println("allowed under Cerone's Ser: " + sol.sat());
  }
}
