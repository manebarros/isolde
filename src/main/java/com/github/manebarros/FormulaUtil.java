package com.github.manebarros;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import kodkod.ast.Decls;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class FormulaUtil {
  public static final ExecutionFormulaG equivalentToHistory(History hist) {
    return (h, co) -> {
      Expression transactions = null;
      Variable v0 = Variable.unary("v0");
      Expression writes = h.initialTransaction().product(h.keys()).product(v0);
      Expression reads = null;
      Expression so = null;
      Map<Integer, Variable> keyVars = new LinkedHashMap<>();
      Map<Integer, Variable> valVars = new LinkedHashMap<>();
      Set<Variable> allTxnVariables = new LinkedHashSet<>();
      valVars.put(0, v0);
      for (int i = 0; i < hist.getSessions().size(); i++) {
        Set<Variable> txnVariables = new LinkedHashSet<>();
        for (int j = 0; j < hist.getSessions().get(i).transactions().size(); j++) {
          Variable t = Variable.unary("t_" + i + j);
          allTxnVariables.add(t);
          so =
              so == null
                  ? h.initialTransaction().product(t)
                  : so.union(h.initialTransaction().product(t));
          transactions = transactions == null ? t : transactions.union(t);
          for (var prevTxn : txnVariables) {
            so = so.union(prevTxn.product(t));
          }
          txnVariables.add(t);
          for (Operation op : hist.getSessions().get(i).transactions().get(j).operations()) {
            int key = op.object();
            int val = op.value();
            if (!keyVars.containsKey(key)) {
              keyVars.put(key, Variable.unary("x" + key));
            }
            if (!valVars.containsKey(val)) {
              valVars.put(val, Variable.unary("v" + val));
            }
            var tuple = t.product(keyVars.get(key)).product(valVars.get(val));
            if (op.isRead()) {
              reads = reads == null ? tuple : reads.union(tuple);
            }
            if (op.isWrite()) {
              writes = writes == null ? tuple : writes.union(tuple);
            }
          }
        }
      }

      Decls decls = v0.oneOf(h.values());
      for (var tvar : allTxnVariables) {
        decls = decls.and(tvar.oneOf(h.normalTxns()));
      }
      for (var kvar : keyVars.values()) {
        decls = decls.and(kvar.oneOf(h.keys()));
      }
      for (var vvar : valVars.values()) {
        decls = decls.and(vvar.oneOf(h.values()));
      }

      System.out.println(transactions);
      System.out.println(writes);
      System.out.println(reads);
      System.out.println(so);

      return Formula.and(
              h.normalTxns().eq(transactions),
              h.finalWrites().eq(writes),
              h.externalReads().eq(reads),
              h.sessionOrder().eq(so),
              KodkodUtil.disj(allTxnVariables),
              KodkodUtil.disj(keyVars.values()),
              KodkodUtil.disj(valVars.values()))
          .forSome(decls);
    };
  }
}
