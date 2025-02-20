package com.github.manebarros;

import static com.github.manebarros.DirectAbstractHistoryEncoding.initialTransaction;
import static com.github.manebarros.DirectAbstractHistoryEncoding.keys;
import static com.github.manebarros.DirectAbstractHistoryEncoding.reads;
import static com.github.manebarros.DirectAbstractHistoryEncoding.sessionOrder;
import static com.github.manebarros.DirectAbstractHistoryEncoding.sessions;
import static com.github.manebarros.DirectAbstractHistoryEncoding.transactions;
import static com.github.manebarros.DirectAbstractHistoryEncoding.txn_session;
import static com.github.manebarros.DirectAbstractHistoryEncoding.values;
import static com.github.manebarros.DirectAbstractHistoryEncoding.writes;
import static com.github.manebarros.KodkodUtil.total;
import static com.github.manebarros.KodkodUtil.transitive;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

public final class DirectSynthesisEncoder implements SynthesisEncoder {
  private DirectSynthesisEncoder() {}

  private static DirectSynthesisEncoder instance = null;

  public static DirectSynthesisEncoder instance() {
    if (instance == null) {
      instance = new DirectSynthesisEncoder();
    }
    return instance;
  }

  @Override
  public Contextualized<KodkodProblem> encode(
      Scope scope,
      List<ExecutionFormulaK<BiswasExecutionK>> biswasFormulas,
      List<ExecutionFormulaK<CeroneExecutionK>> ceroneFormulas) {
    List<Atom<Integer>> txnAtoms =
        IntStream.rangeClosed(0, scope.getTransactions())
            .mapToObj(i -> new Atom<>("t", i))
            .collect(Collectors.toList());

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
    allAtoms.addAll(objAtoms);
    allAtoms.addAll(valAtoms);
    allAtoms.addAll(sessionAtoms);

    Universe u = new Universe(allAtoms);
    Bounds b = new Bounds(u);
    TupleFactory f = u.factory();

    b.boundExactly(transactions, f.setOf(txnAtoms.toArray()));
    b.boundExactly(keys, f.setOf(objAtoms.toArray()));
    b.boundExactly(values, f.setOf(valAtoms.toArray()));
    b.boundExactly(sessions, f.setOf(sessionAtoms.toArray()));

    Atom<Integer> initialTxn = txnAtoms.get(0);
    List<Atom<Integer>> normalTxns = txnAtoms.subList(1, scope.getTransactions() + 1);

    b.boundExactly(initialTransaction, f.setOf(initialTxn));

    TupleSet writesLowerBound =
        f.setOf(initialTxn).product(f.setOf(objAtoms.toArray())).product(f.setOf(valAtoms.get(0)));
    TupleSet writesUpperBound =
        f.setOf(normalTxns.toArray())
            .product(f.setOf(objAtoms.toArray()))
            .product(f.setOf(valAtoms.subList(1, scope.getValues()).toArray()));
    writesUpperBound.addAll(writesLowerBound);
    b.bound(writes, writesLowerBound, writesUpperBound);

    TupleSet readsUpperBound =
        f.setOf(normalTxns.toArray())
            .product(f.setOf(objAtoms.toArray()))
            .product(f.setOf(valAtoms.toArray()));
    b.bound(reads, readsUpperBound);

    TupleSet txnTotalOrderTs = f.noneOf(2);
    // Traverse the txn indexes from the initial txn (i=0) to the penultimate txn
    for (int i = 0; i < scope.getTransactions(); i++) {
      for (int j = i + 1; j < scope.getTransactions() + 1; j++) {
        txnTotalOrderTs.add(f.tuple(txnAtoms.get(i), txnAtoms.get(j)));
      }
    }

    TupleSet sessionOrderLowerBound = f.setOf(initialTxn).product(f.setOf(normalTxns.toArray()));

    b.bound(sessionOrder, sessionOrderLowerBound, txnTotalOrderTs);
    b.bound(txn_session, f.setOf(normalTxns.toArray()).product(f.setOf(sessionAtoms.toArray())));

    var enc = DirectAbstractHistoryEncoding.instance();
    Formula formula =
        Formula.and(
            noBlindWrites(),
            noEmptyTransactions(),
            transactionsWriteToKeyAtMostOnce(),
            transactionsReadKeyAtMostOnce(),
            sessionSemantics(),
            enc.noReadsFromThinAir(),
            uniqueWrites());

    boolean preOrdered = false;

    List<Relation> commitOrderRelations = new ArrayList<>();
    if (!biswasFormulas.isEmpty()) {
      Relation mainCommitOrder = Relation.binary("Commit order #0");
      commitOrderRelations.add(mainCommitOrder);
      b.boundExactly(mainCommitOrder, txnTotalOrderTs);
      preOrdered = true;
      formula =
          formula
              .and(enc.sessionOrder().union(enc.binaryWr()).in(mainCommitOrder))
              .and(
                  biswasFormulas
                      .get(0)
                      .apply(new DefaultBiswasExecutionK(enc, commitOrderRelations.get(0))));

      // TODO: Can we use a more strict upper bound for the remaining commit orders?
      for (int i = 1; i < biswasFormulas.size(); i++) {
        Relation commitOrder = Relation.binary("Commit order #" + i);
        commitOrderRelations.add(commitOrder);
        TupleSet commitOrderTs = f.noneOf(2);
        commitOrderTs.addAll(sessionOrderLowerBound);
        for (int j = 0; j < scope.getTransactions(); j++) {
          for (int k = 0; k < scope.getTransactions(); k++) {
            if (j != k) {
              commitOrderTs.add(f.tuple(normalTxns.get(j), normalTxns.get(k)));
            }
          }
        }
        b.bound(commitOrder, sessionOrderLowerBound, commitOrderTs);
        formula =
            formula
                .and(commitOrderSemantics(commitOrder))
                .and(biswasFormulas.get(i).apply(new DefaultBiswasExecutionK(enc, commitOrder)));
      }
    }

    List<Relation> visRelations = new ArrayList<>();
    List<Relation> arRelations = new ArrayList<>();
    if (!ceroneFormulas.isEmpty()) {
      int i = 0;
      if (!preOrdered) {
        i++;
        Relation mainVisOrder = Relation.binary("Vis #0");
        Relation mainArOrder = Relation.binary("Ar #0");
        visRelations.add(mainVisOrder);
        arRelations.add(mainArOrder);
        b.boundExactly(mainArOrder, txnTotalOrderTs);
        b.bound(mainVisOrder, sessionOrderLowerBound, txnTotalOrderTs);
        formula =
            formula.and(
                ceroneFormulas
                    .get(0)
                    .apply(new DefaultCeroneExecutionK(enc, mainVisOrder, mainArOrder)));
      }

      for (; i < ceroneFormulas.size(); i++) {
        Relation vis = Relation.binary("Vis #" + i);
        Relation ar = Relation.binary("Ar #" + i);
        visRelations.add(vis);
        arRelations.add(ar);
        TupleSet commitOrderTs = Util.irreflexiveBound(f, normalTxns);
        commitOrderTs.addAll(sessionOrderLowerBound);
        b.bound(vis, commitOrderTs);
        b.bound(ar, commitOrderTs);
        formula =
            formula
                .and(vis.in(ar))
                .and(transitive(ar))
                .and(total(ar, enc.transactions()))
                .and(ceroneFormulas.get(i).apply(new DefaultCeroneExecutionK(enc, vis, ar)));
      }
    }

    List<BiswasExecutionK> biswasExecutions =
        commitOrderRelations.stream()
            .map(r -> new DefaultBiswasExecutionK(enc, r))
            .collect(Collectors.toList());
    List<CeroneExecutionK> ceroneExecutions = new ArrayList<>();
    for (int i = 0; i < ceroneFormulas.size(); i++) {
      ceroneExecutions.add(
          new DefaultCeroneExecutionK(enc, visRelations.get(i), arRelations.get(i)));
    }

    return new Contextualized<>(
        enc, biswasExecutions, ceroneExecutions, new KodkodProblem(formula, b));
  }

  private Formula noEmptyTransactions() {
    var e = DirectAbstractHistoryEncoding.instance();
    return e.finalWrites()
        .union(e.externalReads())
        .join(e.values())
        .join(e.keys())
        .eq(e.transactions());
  }

  private Formula noBlindWrites() {
    Variable t = Variable.unary("t");
    return DirectAbstractHistoryEncoding.instance()
        .writeSet(t)
        .in(DirectAbstractHistoryEncoding.instance().readSet(t))
        .forAll(t.oneOf(DirectAbstractHistoryEncoding.instance().normalTxns()));
  }

  private Formula transactionsWriteToKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");
    return x.join(t.join(writes)).lone().forAll(t.oneOf(transactions).and(x.oneOf(keys)));
  }

  private Formula transactionsReadKeyAtMostOnce() {
    Variable t = Variable.unary("t");
    Variable x = Variable.unary("x");
    return x.join(t.join(reads)).lone().forAll(t.oneOf(transactions).and(x.oneOf(keys)));
  }

  private Formula sessionSemantics() {
    Variable s = Variable.unary("s");
    Expression normalTxns = DirectAbstractHistoryEncoding.instance().normalTxns();

    return Formula.and(
        txn_session.function(normalTxns, sessions),
        txn_session.transpose().join(sessionOrder.join(txn_session)).in(Expression.IDEN),
        KodkodUtil.total(sessionOrder, txn_session.join(s)).forAll(s.oneOf(sessions)),
        transitive(sessionOrder));
  }

  private Formula uniqueWrites() {
    Variable x = Variable.unary("x");
    Variable n = Variable.unary("n");

    return writes.join(n).join(x).lone().forAll(x.oneOf(keys).and(n.oneOf(values)));
  }

  private Formula commitOrderSemantics(Expression commitOrder) {
    return Formula.and(
        transitive(commitOrder),
        total(commitOrder, transactions),
        sessionOrder.union(DirectAbstractHistoryEncoding.instance().binaryWr()).in(commitOrder));
  }
}
