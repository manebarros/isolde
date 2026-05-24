package haslab.isolde.cerone;

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

public record CeroneExecution(AbstractHistoryK history, Expression vis, Expression ar)
    implements Execution {

  @Override
  public String showAdditionalStructures(Instance instance) {
    Evaluator eval = new Evaluator(instance);
    Variable t = Variable.unary("t");
    Variable s = Variable.unary("s");

    Expression visWithoutInitialTxn =
        t.product(s)
            .in(vis)
            .comprehension(t.oneOf(history.normalTxns()).and(s.oneOf(history.normalTxns())));

    Expression arWithoutInitialTxn =
        t.product(s)
            .in(ar)
            .comprehension(t.oneOf(history.normalTxns()).and(s.oneOf(history.normalTxns())));

    Map<Integer, Set<Integer>> visMap =
        Util.readBinaryExpression(eval, visWithoutInitialTxn, Integer.class, Integer.class);
    Map<Integer, Set<Integer>> arMap =
        Util.readBinaryExpression(eval, arWithoutInitialTxn, Integer.class, Integer.class);

    return String.format("Vis:\n%s\nAr:\n%s", drawVis(visMap), drawAR(arMap));
  }

  public String drawVis(Map<Integer, Set<Integer>> visMap) {
    StringBuilder sb = new StringBuilder();
    for (Integer tString : visMap.keySet()) {
      for (Integer next_tString : visMap.get(tString))
        sb.append(tString).append(" -> ").append(next_tString).append("\n");
    }
    return sb.toString();
  }

  public String drawAR(Map<Integer, Set<Integer>> arMap) {
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
