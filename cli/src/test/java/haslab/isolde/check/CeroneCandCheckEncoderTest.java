package haslab.isolde.check;

import static org.junit.jupiter.api.Assertions.assertTrue;

import haslab.isolde.cerone.CeroneCandCheckingModuleEncoder;
import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.cerone.definitions.CeroneDefinitions;
import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.check.candidate.DefaultCandCheckingEncoder;
import haslab.isolde.core.check.candidate.DefaultCandidateChecker;
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

  default DefaultCandidateChecker<CeroneExecution> candCheckEncoder() {
    return new DefaultCandidateChecker<>(
        DefaultCandCheckingEncoder.instance(), candCheckModuleEncoder());
  }

  @Test
  default void sessionVisibilityViolationDisallowedBySessionAxiom() {
    var enc = DirectAbstractHistoryEncoding.INSTANCE;
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(Arrays.asList("t0", "t1", "t2"));
    atoms.addAll(Arrays.asList(0, 1));
    atoms.addAll(Arrays.asList("x"));
    atoms.addAll(Arrays.asList("s1"));
    Instance instance = new Instance(new Universe(atoms));
    TupleFactory tf = instance.universe().factory();
    instance.add(enc.transactions(), tf.setOf("t0", "t1", "t2"));
    instance.add(enc.keys(), tf.setOf("x"));
    instance.add(enc.values(), tf.setOf(0, 1));
    instance.add(enc.initialTransaction(), tf.setOf("t0"));
    instance.add(enc.finalWrites(), tf.setOf(tf.tuple("t0", "x", 0), tf.tuple("t1", "x", 1)));
    instance.add(enc.externalReads(), tf.setOf(tf.tuple("t1", "x", 0), tf.tuple("t2", "x", 0)));
    instance.add(enc.sessionOrder(), tf.setOf(tf.tuple("t1", "t2")));
    Solution sol =
        candCheckEncoder()
            .check(
                instance, enc, CeroneDefinitions.EXT.and(CeroneDefinitions.SESSION), new Solver());
    assertTrue(sol.unsat());
  }
}
