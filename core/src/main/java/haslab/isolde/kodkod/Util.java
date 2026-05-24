package haslab.isolde.kodkod;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.core.ExecutionExpression;
import haslab.isolde.core.HistoryExpression;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kodkod.ast.Expression;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Tuple;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

public final class Util {
  private Util() {}

  public static <A> TupleSet irreflexiveBound(TupleFactory f, Collection<A> atoms) {
    var tuples =
        f.setOf(atoms.toArray()).product(f.setOf(atoms.toArray())).stream()
            .filter(t -> !t.atom(0).equals(t.atom(1)))
            .collect(Collectors.toList());
    if (tuples.isEmpty()) {
      return f.noneOf(2);
    } else {
      return f.setOf(tuples);
    }
  }

  public static List<Object> unaryTupleSetToAtoms(TupleSet ts) {
    return ts.stream().map(t -> t.atom(0)).collect(Collectors.toList());
  }

  public static <T> Set<T> readUnaryExpression(Evaluator evaluator, Expression exp, Class<T> type) {
    TupleSet ts = evaluator.evaluate(exp);
    assert ts.arity() == 1;
    Set<T> r = new LinkedHashSet<>(ts.size());
    for (Tuple t : ts) {
      Atom<?> atom = (Atom<?>) t.atom(0);
      r.add(type.cast(atom.value()));
    }
    return r;
  }

  public static <K, V> Map<K, Set<V>> readBinaryExpression(
      Evaluator evaluator, Expression exp, Class<K> keyType, Class<V> valType) {
    TupleSet ts = evaluator.evaluate(exp);
    assert ts.arity() == 2;
    Map<K, Set<V>> mapping = new LinkedHashMap<>();
    for (Tuple tuple : ts) {
      K k = keyType.cast(((Atom<?>) tuple.atom(0)).value());
      V v = valType.cast(((Atom<?>) tuple.atom(1)).value());
      if (!mapping.containsKey(k)) {
        mapping.put(k, new LinkedHashSet<>());
      }
      mapping.get(k).add(v);
    }
    return mapping;
  }

  public static <K, V> Map<K, V> readFunction(
      Evaluator evaluator, Expression exp, Class<K> keyType, Class<V> valType) {
    TupleSet ts = evaluator.evaluate(exp);
    assert ts.arity() == 2;
    Map<K, V> mapping = new LinkedHashMap<>();
    for (Tuple tuple : ts) {
      K k = keyType.cast(((Atom<?>) tuple.atom(0)).value());
      V v = valType.cast(((Atom<?>) tuple.atom(1)).value());
      mapping.put(k, v);
    }
    return mapping;
  }

  public static TupleSet convert(
      Evaluator ev,
      AbstractHistoryK context,
      HistoryExpression expression,
      TupleFactory tf,
      int arity) {
    assert arity > 0;
    TupleSet ts = tf.noneOf(arity);
    for (var tuple : ev.evaluate(expression.resolve(context))) {
      Tuple newTuple = tf.tuple(tuple.atom(0));
      for (int i = 1; i < arity; i++) {
        newTuple = newTuple.product(tf.tuple(tuple.atom(i)));
      }
      ts.add(newTuple);
    }

    return ts;
  }

  public static <E extends Execution> TupleSet convert(
      Evaluator ev, E execution, ExecutionExpression<E> expression, TupleFactory tf, int arity) {
    assert arity > 0;
    TupleSet ts = tf.noneOf(arity);
    for (var tuple : ev.evaluate(expression.resolve(execution))) {
      Tuple newTuple = tf.tuple(tuple.atom(0));
      for (int i = 1; i < arity; i++) {
        newTuple = newTuple.product(tf.tuple(tuple.atom(i)));
      }
      ts.add(newTuple);
    }

    return ts;
  }

  public static void extend(Bounds dest, Bounds src) {
    for (Relation rel : src.relations()) {
      dest.bound(rel, src.lowerBound(rel), src.upperBound(rel));
    }
  }
}
