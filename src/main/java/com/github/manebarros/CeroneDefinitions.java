package com.github.manebarros;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class CeroneDefinitions {

  public static final ExecutionFormulaK<CeroneExecutionK> EXT =
      (h, e) -> {
        Variable T = Variable.unary("T");
        Variable x = Variable.unary("x");
        Variable n = Variable.unary("n");

        Expression wrote_to_x_and_was_seen_by_t =
            e.vis().join(T).intersection(h.txnThatWriteToAnyOf(x));
        Formula first_read = wrote_to_x_and_was_seen_by_t.no().and(n.eq(h.initialValue(x)));
        Expression last_txn_to_write_x = KodkodUtil.max(e.ar(), wrote_to_x_and_was_seen_by_t);
        Formula last_write_to_x_was_n =
            wrote_to_x_and_was_seen_by_t
                .some()
                .and(
                    h.finalWrite(
                        last_txn_to_write_x,
                        x,
                        n)); // guarantees max is only used with a non-empty set

        return (h.externalRead(T, x, n).implies(first_read.or(last_write_to_x_was_n)))
            .forAll(T.oneOf(h.transactions()).and(x.oneOf(h.keys())).and(n.oneOf(h.values())));
      };

  public static final ExecutionFormulaK<CeroneExecutionK> NO_CONFLICT =
      (h, e) -> {
        Variable T = Variable.unary("T");
        Variable S = Variable.unary("S");
        Variable x = Variable.unary("x");

        Formula distinct_transactions = S.eq(T).not();
        Formula both_write_x = h.writes(T, x).and(h.writes(S, x));
        Formula one_sees_the_other = (T.product(S)).in(e.vis()).or((S.product(T)).in(e.vis()));

        return distinct_transactions
            .and(both_write_x)
            .implies(one_sees_the_other)
            .forAll(
                T.oneOf(h.transactions()).and(S.oneOf(h.transactions())).and(x.oneOf(h.keys())));
      };

  public static final ExecutionFormulaK<CeroneExecutionK> TRANS_VIS =
      (h, e) -> KodkodUtil.transitive(e.vis());

  public static final ExecutionFormulaK<CeroneExecutionK> PREFIX =
      (h, e) -> e.ar().join(e.vis()).in(e.vis());

  public static final ExecutionFormulaK<CeroneExecutionK> TOTAL_VIS =
      (h, e) -> KodkodUtil.total(e.vis(), h.transactions());

  public static final ExecutionFormulaK<CeroneExecutionK> SESSION =
      (h, e) -> h.sessionOrder().in(e.vis());

  public static final ExecutionFormulaK<CeroneExecutionK> RA = EXT.and(SESSION);
  public static final ExecutionFormulaK<CeroneExecutionK> CC = EXT.and(SESSION).and(TRANS_VIS);
  public static final ExecutionFormulaK<CeroneExecutionK> UA = EXT.and(SESSION).and(NO_CONFLICT);
  public static final ExecutionFormulaK<CeroneExecutionK> PSI =
      EXT.and(SESSION).and(TRANS_VIS).and(NO_CONFLICT);
  public static final ExecutionFormulaK<CeroneExecutionK> PC = EXT.and(SESSION).and(PREFIX);
  public static final ExecutionFormulaK<CeroneExecutionK> SI =
      EXT.and(SESSION).and(PREFIX).and(NO_CONFLICT);
  public static final ExecutionFormulaK<CeroneExecutionK> SER = EXT.and(TOTAL_VIS);
}
