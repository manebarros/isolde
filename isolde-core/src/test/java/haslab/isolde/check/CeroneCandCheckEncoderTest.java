package haslab.isolde.check;

import static haslab.isolde.core.DirectAbstractHistoryEncoding.initialTransaction;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.keys;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.reads;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.sessionOrder;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.sessions;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.transactions;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.txn_session;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.values;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.writes;
import static org.junit.jupiter.api.Assertions.assertTrue;

import haslab.isolde.cerone.CeroneCandCheckingModuleEncoder;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.check.candidate.CandChecker;
import haslab.isolde.core.check.candidate.DefaultCandCheckingEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;
import kodkod.instance.Universe;
import org.junit.jupiter.api.Test;

public interface CeroneCandCheckEncoderTest {

  CeroneCandCheckingModuleEncoder candCheckModuleEncoder();

  default CandChecker<CeroneExecution> candCheckEncoder() {
    return new CandChecker<>(DefaultCandCheckingEncoder.instance(), candCheckModuleEncoder());
  }

  @Test
  default void sessionVisibilityViolationDisallowedBySessionAxiom() {
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(Arrays.asList("t0", "t1", "t2"));
    atoms.addAll(Arrays.asList(0, 1));
    atoms.addAll(Arrays.asList("x"));
    atoms.addAll(Arrays.asList("s1"));
    Instance instance = new Instance(new Universe(atoms));
    TupleFactory tf = instance.universe().factory();
    instance.add(transactions, tf.setOf("t0", "t1", "t2"));
    instance.add(keys, tf.setOf("x"));
    instance.add(values, tf.setOf(0, 1));
    instance.add(sessions, tf.setOf("s1"));
    instance.add(initialTransaction, tf.setOf("t0"));
    instance.add(writes, tf.setOf(tf.tuple("t0", "x", 0), tf.tuple("t1", "x", 1)));
    instance.add(reads, tf.setOf(tf.tuple("t1", "x", 0), tf.tuple("t2", "x", 0)));
    instance.add(sessionOrder, tf.setOf(tf.tuple("t1", "t2")));
    instance.add(txn_session, tf.setOf(tf.tuple("t1", "s1"), tf.tuple("t2", "s1")));
    Solution sol =
        candCheckEncoder()
            .check(
                instance,
                DirectAbstractHistoryEncoding.instance(),
                CeroneDefinitions.EXT.and(CeroneDefinitions.SESSION),
                new Solver());
    assertTrue(sol.unsat());
  }
}
