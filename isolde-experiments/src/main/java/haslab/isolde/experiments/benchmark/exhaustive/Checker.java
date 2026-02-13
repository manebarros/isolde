package haslab.isolde.experiments.benchmark.exhaustive;

import haslab.isolde.kodkod.Atom;
import java.util.ArrayList;
import java.util.List;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;

public class Checker {
  private static final Relation transactions = Relation.unary("transactions");
  private static final Relation objects = Relation.unary("objects");
  private static final Relation values = Relation.unary("values");

  private static final Relation reads = Relation.ternary("reads");
  private static final Relation writes = Relation.ternary("writes");
  private static final Relation so = Relation.ternary("so");
  private static final Relation co = Relation.ternary("co");

  private static final Formula baseConstraints() {
    return null;
  }

  private static final Bounds generateBounds(AbstractExecution execution) {
    List<Atom<Integer>> txnAtoms = new ArrayList<>();
    List<Atom<Integer>> keyAtoms = new ArrayList<>();
    List<Atom<Integer>> valAtoms = new ArrayList<>();

    for (int i = 0; i < execution.getTransactions().size(); i++) {
      txnAtoms.add(new Atom<Integer>("t", i));
    }
    for (int i = 0; i < execution.getTransactions().size(); i++) {
      txnAtoms.add(new Atom<Integer>("t", i));
    }
    for (int i = 0; i < execution.getTransactions().size(); i++) {
      txnAtoms.add(new Atom<Integer>("t", i));
    }
  }
}
