package haslab.isolde.biswas;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.Execution;
import haslab.isolde.kodkod.Util;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kodkod.ast.Expression;
import kodkod.ast.Variable;
import kodkod.engine.Evaluator;
import kodkod.instance.Instance;

public record BiswasExecution(AbstractHistoryK history, Expression co) implements Execution {

  @Override
  public String showAdditionalStructures(Instance instance) {
    Evaluator eval = new Evaluator(instance);

    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");
    Expression coWithoutInitialTxn =
        t.product(s)
            .in(co)
            .comprehension(t.oneOf(history.normalTxns()).and(s.oneOf(history.normalTxns())));

    Map<Integer, Set<Integer>> coMap =
        Util.readBinaryExpression(eval, coWithoutInitialTxn, Integer.class, Integer.class);
    return String.format("Commit Order:\n%s", drawCo(coMap));
  }

  public String drawCo(Map<Integer, Set<Integer>> arMap) {
    Set<Integer> everyTxn = new HashSet<>(arMap.keySet());
    for (Set<Integer> nexts : arMap.values()) everyTxn.addAll(nexts);

    List<Integer> arSequence =
        everyTxn.stream()
            .sorted(
                (t1, t2) ->
                    arMap.getOrDefault(t2, new HashSet<>()).size()
                        - arMap.getOrDefault(t1, new HashSet<>()).size())
            .collect(Collectors.toList());

    StringBuilder sb = new StringBuilder();
    ListIterator<Integer> it = arSequence.listIterator();
    while (it.hasNext()) {
      Integer tString = it.next();
      sb.append(tString);
      if (it.hasNext()) sb.append(" -> ");
    }

    return sb.toString();
  }
}
