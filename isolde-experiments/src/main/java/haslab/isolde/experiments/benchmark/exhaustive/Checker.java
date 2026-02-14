package haslab.isolde.experiments.benchmark.exhaustive;

import haslab.isolde.biswas.BiswasExecution;
import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.kodkod.Atom;
import haslab.isolde.kodkod.KodkodProblem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public class Checker {
  private static final Relation initialTransaction = Relation.unary("initial transaction");
  private static final Relation transactions = Relation.unary("transactions");
  private static final Relation objects = Relation.unary("objects");
  private static final Relation values = Relation.unary("values");

  private static final Relation reads = Relation.ternary("reads");
  private static final Relation writes = Relation.ternary("writes");
  private static final Relation so = Relation.ternary("so");
  private static final Relation co = Relation.ternary("co");

  private Solver solver = new Solver();

  public Checker() {
    this(SATFactory.MiniSat);
  }

  public Checker(SATFactory solver) {
    Options options = new Options();
    options.setSolver(solver);
    this.solver = new Solver(options);
  }

  private static final AbstractHistoryRel encoding() {
    return new AbstractHistoryRel() {

      @Override
      public Expression keys() {
        return objects;
      }

      @Override
      public Expression values() {
        return values;
      }

      @Override
      public Expression externalReads() {
        return reads;
      }

      @Override
      public Expression finalWrites() {
        return writes;
      }

      @Override
      public Expression sessionOrder() {
        return so;
      }

      @Override
      public Relation transactions() {
        return transactions;
      }

      @Override
      public Relation initialTransaction() {
        return initialTransaction;
      }
    };
  }

  private static Formula uniqueWrites() {
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    return writes.join(n).join(x).lone().forAll(x.oneOf(objects).and(n.oneOf(values)));
  }

  private static final Formula formula(ExecutionFormula<BiswasExecution> formula) {
    AbstractHistoryK encoding = encoding();
    return Formula.and(
        uniqueWrites(),
        encoding.sessionOrder().union(encoding.wr()).in(co),
        encoding.noReadsFromThinAir(),
        formula.resolve(new BiswasExecution(encoding, co)));
  }

  private static final Bounds generateBounds(AbstractExecution execution) {
    Atom<Integer> initialTxnAtom = new Atom<>("t", -1);
    List<Atom<Integer>> txnAtoms = new ArrayList<>();
    Map<Integer, Atom<Integer>> keyAtoms = new HashMap<>();
    Map<Integer, Atom<Integer>> valAtoms = new HashMap<>();

    for (int i = 0; i < execution.getTransactions().size(); i++) {
      txnAtoms.add(new Atom<>("t", i));
    }
    for (var key : execution.keySet()) {
      keyAtoms.put(key, new Atom<>("k", key));
    }
    for (var val : execution.valueSet()) {
      valAtoms.put(val, new Atom<>("v", val));
    }
    valAtoms.put(0, new Atom<>("v", 0));

    Set<Atom<?>> allAtoms = new HashSet<>();
    allAtoms.add(initialTxnAtom);
    allAtoms.addAll(txnAtoms);
    allAtoms.addAll(keyAtoms.values());
    allAtoms.addAll(valAtoms.values());
    Universe universe = new Universe(allAtoms);
    Bounds bounds = new Bounds(universe);
    TupleFactory factory = universe.factory();

    Set<Atom<Integer>> allTransactions = new HashSet<>(txnAtoms);
    allTransactions.add(initialTxnAtom);
    bounds.boundExactly(initialTransaction, factory.setOf(initialTxnAtom));
    bounds.boundExactly(transactions, factory.setOf(allTransactions.toArray()));
    bounds.boundExactly(objects, factory.setOf(keyAtoms.values().toArray()));
    bounds.boundExactly(values, factory.setOf(valAtoms.values().toArray()));

    // reads and writes
    TupleSet readsTs = factory.noneOf(3);
    TupleSet writesTs = factory.noneOf(3);
    for (int i = 0; i < execution.getTransactions().size(); i++) {
      var transaction = execution.getTransactions().get(i);
      var txnAtom = txnAtoms.get(i);
      for (var entry : transaction.getReads().entrySet()) {
        var keyAtom = keyAtoms.get(entry.getKey());
        var valAtom = valAtoms.get(entry.getValue());
        readsTs.add(factory.tuple(txnAtom, keyAtom, valAtom));
      }
      for (var entry : transaction.getWrites().entrySet()) {
        var keyAtom = keyAtoms.get(entry.getKey());
        var valAtom = valAtoms.get(entry.getValue());
        writesTs.add(factory.tuple(txnAtom, keyAtom, valAtom));
      }
    }
    for (var keyAtom : keyAtoms.values()) {
      writesTs.add(factory.tuple(initialTxnAtom, keyAtom, valAtoms.get(0)));
    }

    // session order
    TupleSet soTs = factory.noneOf(2);
    // normal transactions
    for (var session : execution.getSessionOrder()) {
      for (int i = 0; i < session.size() - 1; i++) {
        for (int j = i + 1; i < session.size(); j++) {
          soTs.add(factory.tuple(txnAtoms.get(i), txnAtoms.get(j)));
        }
      }
    }
    // initial txn -> normal txns
    for (var txnAtom : txnAtoms) {
      soTs.add(factory.tuple(initialTxnAtom, txnAtom));
    }

    // commit-order
    TupleSet coTs = factory.noneOf(2);
    // normal transactions
    for (int i = 0; i < execution.getCommitOrder().size() - 1; i++) {
      for (int j = i + 1; i < execution.getCommitOrder().size(); j++) {
        coTs.add(factory.tuple(txnAtoms.get(i), txnAtoms.get(j)));
      }
    }
    for (var txnAtom : txnAtoms) {
      coTs.add(factory.tuple(initialTxnAtom, txnAtom));
    }

    bounds.boundExactly(reads, readsTs);
    bounds.boundExactly(writes, writesTs);
    bounds.boundExactly(so, soTs);
    bounds.boundExactly(co, coTs);

    return bounds;
  }

  public boolean check(
      AbstractExecution execution,
      ExecutionFormula<BiswasExecution> pos,
      ExecutionFormula<BiswasExecution> neg) {
    Bounds bounds = generateBounds(execution);
    KodkodProblem p1 = new KodkodProblem(formula(pos), bounds);
    KodkodProblem p2 = new KodkodProblem(formula(neg), bounds);
    return p1.solve(this.solver).sat() && p2.solve(this.solver).unsat();
  }
}
