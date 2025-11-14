package haslab.isolde.experiments;

import static haslab.isolde.IsoldeConstraint.biswas;

import haslab.isolde.IsoldeSpec;
import haslab.isolde.IsoldeSynthesizer;
import haslab.isolde.SynthesizedHistory;
import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.biswas.definitions.AxiomaticDefinitions;
import haslab.isolde.core.synth.Scope;
import haslab.isolde.experiments.benchmark.Benchmark;
import haslab.isolde.experiments.verification.FeketeReadOnlyAnomaly;
import haslab.isolde.experiments.verification.VerifyPlumeDefinitions;
import kodkod.ast.Formula;
import kodkod.ast.Variable;
import kodkod.engine.satlab.SATFactory;

public class Main {
  private static void plume() {
    Scope s = new Scope.Builder().txn(4).obj(2).val(3).sess(2).build();
    VerifyPlumeDefinitions.verify(s);
  }

  public static final Formula updateSerExplicit(BiswasExecution e) {
    Variable t1 = Variable.unary("t1");
    Variable t2 = Variable.unary("t2");
    Variable t3 = Variable.unary("t3");
    Variable x = Variable.unary("x");

    return Formula.and(
            t1.eq(t2).not(),
            e.history().wr(t1, x, t3),
            t2.product(t3).in(e.co()),
            t2.union(t3).in(e.history().updateTransactions()))
        .implies(t1.in(t2.join(e.co())))
        .forAll(
            x.oneOf(e.history().keys())
                .and(t1.oneOf(e.history().transactions()))
                .and(t2.oneOf(e.history().transactions()))
                .and(t3.oneOf(e.history().transactions())));
  }

  public static void main(String[] args) throws Exception {
    IsoldeSpec spec =
        biswas(FeketeReadOnlyAnomaly::updateSer)
            .and(biswas(AxiomaticDefinitions.Snapshot))
            .andNot(biswas(AxiomaticDefinitions.Ser))
            .build();

    IsoldeSynthesizer synthesizer =
        new IsoldeSynthesizer.Builder()
            .solver(SATFactory.MiniSat)
            .incrementalSolving(true)
            .useTxnTotalOrder(true)
            .build();

    Scope scope = new Scope.Builder(3).obj(2).val(2).build();
    SynthesizedHistory synthesisResult = synthesizer.synthesize(scope, spec);
    System.out.println(synthesisResult);
    Benchmark.runAllExperiments();
  }
}
