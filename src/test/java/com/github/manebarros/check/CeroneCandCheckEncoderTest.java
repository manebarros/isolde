package com.github.manebarros.check;

import static com.github.manebarros.core.DirectAbstractHistoryEncoding.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.manebarros.cerone.CeroneExecution;
import com.github.manebarros.cerone.definitions.CeroneDefinitions;
import com.github.manebarros.core.DirectAbstractHistoryEncoding;
import com.github.manebarros.core.check.DefaultHistoryCheckingEncoder;
import com.github.manebarros.core.check.candidate.CandCheckEncoder;
import com.github.manebarros.core.check.candidate.CandCheckModuleEncoder;
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

  CandCheckModuleEncoder<CeroneExecution> moduleEncoder();

  default CandCheckEncoder<CeroneExecution> candCheckEncoder() {
    return new CandCheckEncoder<>(DefaultHistoryCheckingEncoder.instance(), moduleEncoder());
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
            .solve(
                instance,
                DirectAbstractHistoryEncoding.instance(),
                CeroneDefinitions.EXT.and(CeroneDefinitions.SESSION),
                new Solver());
    assertTrue(sol.unsat());
  }
}
