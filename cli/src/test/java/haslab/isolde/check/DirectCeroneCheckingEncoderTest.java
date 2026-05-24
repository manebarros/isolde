package haslab.isolde.check;

import static haslab.isolde.core.DirectAbstractHistoryEncoding.initialTransaction;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.keys;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.reads;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.sessionOrder;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.transactions;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.values;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.writes;
import static haslab.isolde.history.Operation.readOf;
import static haslab.isolde.history.Operation.writeOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import haslab.isolde.cerone.CeroneCandCheckingModuleEncoder;
import haslab.isolde.cerone.CeroneHistCheckingModuleEncoder;
import haslab.isolde.core.check.external.HistCheckEncoder;
import haslab.isolde.history.History;
import haslab.isolde.history.Session;
import haslab.isolde.history.Transaction;
import haslab.isolde.kodkod.Atom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;
import org.junit.jupiter.api.Test;

public class DirectCeroneCheckingEncoderTest
    implements CeroneCheckingEncoderTest, CeroneCandCheckEncoderTest {

  @Override
  public CeroneCandCheckingModuleEncoder candCheckModuleEncoder() {
    return new CeroneCandCheckingModuleEncoder(1);
  }

  @Override
  public CeroneHistCheckingModuleEncoder histCheckModuleEncoder(int executions) {
    return new CeroneHistCheckingModuleEncoder(executions);
  }

  @Test
  public void historyWithOneTxnGetsWellEncoded() {
    History hist =
        new History(
            Arrays.asList(
                new Session(new Transaction(1, Arrays.asList(readOf(0, 0), writeOf(0, 1))))));

    Relation arAux = Relation.binary("Ar's transitive reduction");
    Relation vis = Relation.binary("vis");

    Bounds b =
        new HistCheckEncoder<>(new CeroneHistCheckingModuleEncoder(vis, arAux))
            .encode(hist, e -> Formula.TRUE)
            .bounds();

    Atom<Integer> initTxnAtom = new Atom<>("t", 0);
    Atom<Integer> txnAtom = new Atom<>("t", 1);
    Atom<Integer> keyAtom = new Atom<Integer>("x", 0);
    Atom<Integer> initialValueAtom = new Atom<Integer>("v", 0);
    Atom<Integer> valAtom = new Atom<Integer>("v", 1);
    // Universe u =
    //    new Universe(initTxnAtom, txnAtom, sessionAtom, keyAtom, initialValueAtom, valAtom);
    Universe u = b.universe();
    TupleFactory f = u.factory();
    Map<Relation, TupleSet> expectedTupleSets =
        Map.of(
            transactions, f.setOf(initTxnAtom, txnAtom),
            keys, f.setOf(keyAtom),
            values, f.setOf(initialValueAtom, valAtom),
            initialTransaction, f.setOf(initTxnAtom),
            reads, f.setOf(f.tuple(txnAtom, keyAtom, initialValueAtom)),
            writes,
                f.setOf(
                    f.tuple(initTxnAtom, keyAtom, initialValueAtom),
                    f.tuple(txnAtom, keyAtom, valAtom)),
            sessionOrder, f.setOf(f.tuple(initTxnAtom, txnAtom)));

    for (Relation rel : expectedTupleSets.keySet()) {
      assertEquals(expectedTupleSets.get(rel), b.lowerBound(rel));
      assertEquals(expectedTupleSets.get(rel), b.upperBound(rel));
    }

    TupleSet arAuxUpperBound = f.setOf(f.tuple(initTxnAtom, txnAtom));
    assertEquals(f.noneOf(2), b.lowerBound(arAux));
    assertEquals(arAuxUpperBound, b.upperBound(arAux));

    TupleSet visExactBound = f.setOf(f.tuple(initTxnAtom, txnAtom));
    assertEquals(visExactBound, b.lowerBound(vis));
    assertEquals(visExactBound, b.upperBound(vis));
  }

  @Test
  public void historyWithTwoTxnGetsWellEncoded() {
    History hist =
        new History(
            Arrays.asList(
                new Session(new Transaction(1, Arrays.asList(readOf(0, 0), writeOf(0, 1)))),
                new Session(new Transaction(2, Arrays.asList(readOf(0, 1), writeOf(0, 2))))));

    Relation arAux = Relation.binary("Ar's transitive reduction");
    Relation vis = Relation.binary("vis");
    Bounds b =
        new HistCheckEncoder<>(new CeroneHistCheckingModuleEncoder(vis, arAux))
            .encode(hist, e -> Formula.TRUE)
            .bounds();

    List<Atom<Integer>> txnAtoms =
        Arrays.asList(new Atom<>("t", 0), new Atom<>("t", 1), new Atom<>("t", 2));
    Atom<Integer> keyAtom = new Atom<Integer>("x", 0);
    List<Atom<Integer>> valAtoms =
        Arrays.asList(new Atom<>("v", 0), new Atom<>("v", 1), new Atom<>("v", 2));

    Universe u = b.universe();
    TupleFactory f = u.factory();
    Map<Relation, TupleSet> expectedTupleSets =
        Map.of(
            transactions, f.setOf(txnAtoms.toArray()),
            keys, f.setOf(keyAtom),
            values, f.setOf(valAtoms.toArray()),
            initialTransaction, f.setOf(txnAtoms.get(0)),
            reads,
                f.setOf(
                    f.tuple(txnAtoms.get(1), keyAtom, valAtoms.get(0)),
                    f.tuple(txnAtoms.get(2), keyAtom, valAtoms.get(1))),
            writes,
                f.setOf(
                    f.tuple(txnAtoms.get(0), keyAtom, valAtoms.get(0)),
                    f.tuple(txnAtoms.get(1), keyAtom, valAtoms.get(1)),
                    f.tuple(txnAtoms.get(2), keyAtom, valAtoms.get(2))),
            sessionOrder,
                f.setOf(
                    f.tuple(txnAtoms.get(0), txnAtoms.get(1)),
                    f.tuple(txnAtoms.get(0), txnAtoms.get(2))));

    for (Relation rel : expectedTupleSets.keySet()) {
      assertEquals(expectedTupleSets.get(rel), b.lowerBound(rel));
      assertEquals(expectedTupleSets.get(rel), b.upperBound(rel));
    }

    TupleSet arAuxUpperBound =
        f.setOf(
            f.tuple(txnAtoms.get(0), txnAtoms.get(1)),
            f.tuple(txnAtoms.get(0), txnAtoms.get(2)),
            f.tuple(txnAtoms.get(1), txnAtoms.get(2)),
            f.tuple(txnAtoms.get(2), txnAtoms.get(1)));
    assertEquals(f.noneOf(2), b.lowerBound(arAux));
    assertEquals(arAuxUpperBound, b.upperBound(arAux));

    TupleSet visExpectedLowerBound =
        f.setOf(
            f.tuple(txnAtoms.get(0), txnAtoms.get(1)), f.tuple(txnAtoms.get(0), txnAtoms.get(2)));
    TupleSet visExpectedUpperBound =
        f.setOf(
            f.tuple(txnAtoms.get(0), txnAtoms.get(1)),
            f.tuple(txnAtoms.get(0), txnAtoms.get(2)),
            f.tuple(txnAtoms.get(1), txnAtoms.get(2)),
            f.tuple(txnAtoms.get(2), txnAtoms.get(1)));
    assertEquals(visExpectedLowerBound, b.lowerBound(vis));
    assertEquals(visExpectedUpperBound, b.upperBound(vis));
  }
}
