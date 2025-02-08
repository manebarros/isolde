package com.github.manebarros;

import static com.github.manebarros.HistoryEncoding.Event;
import static com.github.manebarros.HistoryEncoding.InitialValue;
import static com.github.manebarros.HistoryEncoding.Object;
import static com.github.manebarros.HistoryEncoding.Read;
import static com.github.manebarros.HistoryEncoding.Session;
import static com.github.manebarros.HistoryEncoding.Transaction;
import static com.github.manebarros.HistoryEncoding.Value;
import static com.github.manebarros.HistoryEncoding.Write;
import static com.github.manebarros.HistoryEncoding.events;
import static com.github.manebarros.HistoryEncoding.object;
import static com.github.manebarros.HistoryEncoding.programOrder;
import static com.github.manebarros.HistoryEncoding.session;
import static com.github.manebarros.HistoryEncoding.sessionOrder;
import static com.github.manebarros.HistoryEncoding.value;
import static com.github.manebarros.HistoryEncoding.wr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.engine.Evaluator;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public class Synthesizer {

  public Solution synthesize(
      Scope scope, List<ExecutionFormula> existentialFormulas, ExecutionFormula universalFormula) {
    Context<Bounds> b = generateBounds(scope, existentialFormulas, universalFormula);
    Formula f = generateFormula(b.getCommitOrders(), existentialFormulas, universalFormula);
    IncrementalSolver synthesizer = IncrementalSolver.solver(new Options());
    Solver checker = new Solver();
    Solution candSol = synthesizer.solve(f, b.getContent());
    if (candSol.unsat()) {
      return candSol;
    }
    Context<Solution> candCheckSol = verify(checker, candSol.instance(), universalFormula);
    while (candCheckSol.getContent().sat()
        && (candSol =
                nextCandidate(synthesizer, candCheckSol.fmap(Solution::instance), universalFormula))
            .sat()) {
      candCheckSol = verify(checker, candSol.instance(), universalFormula);
    }
    return candSol;
  }

  private Formula generateFormula(
      List<Expression> commitOrders,
      List<ExecutionFormula> existentialFormulas,
      ExecutionFormula universalFormula) {
    return Formula.and(
        soSemantics(),
        uniqueWrites(),
        noReadsFromThinAir(),
        Write.union(Read).eq(Event), // Write + Read = Event
        Write.intersection(Read).no(), // no (Write & Read)
        HistoryEncoding.object.function(
            HistoryEncoding.Event,
            HistoryEncoding.Object), // `object` is a function mapping events to their objects
        HistoryEncoding.value.function(
            HistoryEncoding.Event,
            HistoryEncoding.Value), // `value` is a function mapping events to their values
        session.function(HistoryEncoding.Transaction, Session));
  }

  private Formula noReadsFromThinAir() {
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    Variable x = Variable.unary("x");
    Variable v = Variable.unary("v");

    return HistoryEncoding.externalRead(t, x, v)
        .implies(
            s.product(x).product(t).in(wr()).forSome(s.oneOf(Transaction)).or(v.eq(InitialValue)))
        .forAll(t.oneOf(Transaction).and(x.oneOf(Object).and(v.oneOf(Value))));
  }

  private Formula uniqueWrites() {
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    Formula noTxnWritesTheInitialValue =
        HistoryEncoding.value.join(HistoryEncoding.InitialValue).intersection(Write).no();

    return HistoryEncoding.events
        .join(
            HistoryEncoding.object
                .join(x)
                .intersection(HistoryEncoding.Write)
                .intersection(HistoryEncoding.value.join(n)))
        .lone()
        .forAll(x.oneOf(HistoryEncoding.Object).and(n.oneOf(HistoryEncoding.Value)))
        .and(noTxnWritesTheInitialValue);
  }

  private Formula soSemantics() {
    Variable s = Variable.unary("s");
    return Formula.and(
        session.transpose().join(sessionOrder.join(session)).in(Expression.IDEN),
        KodkodUtil.total(sessionOrder, session.join(s)).forAll(s.oneOf(Session)),
        KodkodUtil.transitive(sessionOrder));
  }

  private Context<Solution> verify(Solver solver, Instance candidate, ExecutionFormula formula) {
    Universe u = candidate.universe();
    Bounds b = new Bounds(u);
    Evaluator ev = new Evaluator(candidate);
    TupleFactory f = u.factory();

    b.boundExactly(Transaction, ev.evaluate(Transaction));
    b.boundExactly(Event, ev.evaluate(Event));
    b.boundExactly(Object, ev.evaluate(Object));
    b.boundExactly(Value, ev.evaluate(Value));
    b.boundExactly(Session, ev.evaluate(Session));

    b.boundExactly(Write, ev.evaluate(Write));
    b.boundExactly(Read, ev.evaluate(Read));

    b.boundExactly(events, ev.evaluate(events));
    b.boundExactly(object, ev.evaluate(object));
    b.boundExactly(value, ev.evaluate(value));

    b.boundExactly(programOrder, ev.evaluate(programOrder));
    b.boundExactly(sessionOrder, ev.evaluate(sessionOrder));
    b.boundExactly(session, ev.evaluate(session));

    Relation commitOrderAux = Relation.binary("CO Aux");
    Relation firstTxn = Relation.unary("fst");
    Relation lastTxn = Relation.unary("lst");
    Relation commitOrder = Relation.binary("counterexample's commit order");

    b.bound(
        commitOrderAux,
        Util.irreflexiveBound(f, Util.unaryTupleSetToAtoms(ev.evaluate(Transaction))));
    b.bound(
        commitOrder,
        calculateCommitOrderLowerBound(ev),
        Util.irreflexiveBound(f, Util.unaryTupleSetToAtoms(ev.evaluate(Transaction))));
    b.bound(firstTxn, ev.evaluate(Transaction));
    b.bound(lastTxn, ev.evaluate(Transaction));

    Formula spec =
        Formula.and(
            commitOrderAux.totalOrder(Transaction, firstTxn, lastTxn),
            commitOrder.eq(commitOrderAux.closure()),
            formula.apply(commitOrder).not());

    Solution sol = solver.solve(spec, b);
    return new Context<Solution>(Collections.singletonList(commitOrder), sol);
  }

  private TupleSet calculateCommitOrderLowerBound(Evaluator evaluator) {
    return evaluator.evaluate(HistoryEncoding.causaOrder());
  }

  private Context<Bounds> generateBounds(
      Scope scope, List<ExecutionFormula> existentialFormulas, ExecutionFormula universalFormula) {
    List<Atom<Integer>> txnAtoms =
        IntStream.rangeClosed(1, scope.getTransactions())
            .mapToObj(i -> new Atom<>("t", i))
            .collect(Collectors.toList());

    List<Atom<String>> eventAtoms =
        new ArrayList<>(scope.getTransactions() * scope.getOperations());
    for (int i = 1; i <= scope.getTransactions(); i++) {
      for (int j = 0; j < scope.getOperations(); j++) {
        eventAtoms.add(new Atom<>("e", i + "_" + j));
      }
    }

    List<Atom<Integer>> objAtoms =
        IntStream.range(0, scope.getObjects())
            .mapToObj(i -> new Atom<>("x", i))
            .collect(Collectors.toList());

    List<Atom<Integer>> valAtoms =
        IntStream.range(0, scope.getValues())
            .mapToObj(i -> new Atom<>("v", i))
            .collect(Collectors.toList());

    List<Atom<Integer>> sessionAtoms =
        IntStream.range(0, scope.getSessions())
            .mapToObj(i -> new Atom<>("s", i))
            .collect(Collectors.toList());

    List<Atom<?>> allAtoms = new ArrayList<>();
    allAtoms.addAll(txnAtoms);
    allAtoms.addAll(eventAtoms);
    allAtoms.addAll(objAtoms);
    allAtoms.addAll(valAtoms);
    allAtoms.addAll(sessionAtoms);

    Universe u = new Universe(allAtoms);
    Bounds b = new Bounds(u);
    TupleFactory f = u.factory();

    b.boundExactly(HistoryEncoding.Transaction, f.setOf(txnAtoms.toArray()));
    b.boundExactly(Event, f.setOf(eventAtoms.toArray()));
    b.boundExactly(HistoryEncoding.Object, f.setOf(objAtoms.toArray()));
    b.boundExactly(HistoryEncoding.Value, f.setOf(valAtoms.toArray()));
    b.boundExactly(Session, f.setOf(sessionAtoms.toArray()));

    b.bound(Write, f.setOf(eventAtoms.toArray()));
    b.bound(Read, f.setOf(eventAtoms.toArray()));

    b.boundExactly(HistoryEncoding.InitialValue, f.setOf(valAtoms.get(0)));

    TupleSet eventsTs = f.noneOf(2);
    TupleSet programOrderTs = f.noneOf(2);
    int g = 0;
    for (var txnAtom : txnAtoms) {
      for (int i = 0; i < scope.getOperations(); i++) {
        eventsTs.add(f.tuple(txnAtom, eventAtoms.get(g + i)));
        for (int j = i + 1; j < scope.getOperations(); j++) {
          programOrderTs.add(f.tuple(eventAtoms.get(g + i), eventAtoms.get(g + j)));
        }
      }
      g += scope.getOperations();
    }
    b.boundExactly(HistoryEncoding.events, eventsTs);
    b.boundExactly(HistoryEncoding.programOrder, programOrderTs);

    b.bound(
        HistoryEncoding.object, f.setOf(eventAtoms.toArray()).product(f.setOf(objAtoms.toArray())));
    b.bound(
        HistoryEncoding.value, f.setOf(eventAtoms.toArray()).product(f.setOf(valAtoms.toArray())));

    List<Expression> commitOrderRelations =
        new ArrayList<>(existentialFormulas.isEmpty() ? 1 : existentialFormulas.size());

    TupleSet mainCommitOrderTs = f.noneOf(2);
    Relation mainCommitOrder = Relation.binary("Commit order #0");

    commitOrderRelations.add(mainCommitOrder);

    for (int i = 0; i < scope.getTransactions() - 1; i++) {
      for (int j = i + 1; j < scope.getTransactions(); j++) {
        mainCommitOrderTs.add(f.tuple(txnAtoms.get(i), txnAtoms.get(j)));
      }
    }
    b.boundExactly(mainCommitOrder, mainCommitOrderTs);

    for (int i = 1; i < existentialFormulas.size(); i++) {
      Relation commitOrder = Relation.binary("Commit order #" + i);
      commitOrderRelations.add(commitOrder);
      TupleSet commitOrderTs = f.noneOf(2);
      for (int j = 0; i < scope.getTransactions(); i++) {
        for (int k = 0; k < scope.getTransactions(); k++) {
          if (j != k) {
            commitOrderTs.add(f.tuple(txnAtoms.get(j), txnAtoms.get(k)));
          }
        }
      }
    }

    b.bound(sessionOrder, mainCommitOrderTs);
    b.bound(session, f.setOf(txnAtoms.toArray()).product(f.setOf(sessionAtoms.toArray())));

    return new Context<>(commitOrderRelations, b);
  }

  private Solution nextCandidate(
      IncrementalSolver solver,
      Context<Instance> counterexample,
      ExecutionFormula universalFormula) {
    TupleSet commitOrderVal =
        new Evaluator(counterexample.getContent())
            .evaluate(counterexample.getCommitOrders().get(0));
    Relation cexCommitOrderRel = Relation.binary("cexCommitOrder");
    Bounds b = new Bounds(commitOrderVal.universe());
    b.boundExactly(cexCommitOrderRel, commitOrderVal);
    Formula f = universalFormula.apply(cexCommitOrderRel);
    return solver.solve(f, b);
  }
}
