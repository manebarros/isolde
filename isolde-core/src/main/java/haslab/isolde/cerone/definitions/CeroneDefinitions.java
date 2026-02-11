package haslab.isolde.cerone.definitions;

import haslab.isolde.cerone.CeroneExecution;
import haslab.isolde.core.ExecutionFormula;
import haslab.isolde.kodkod.KodkodUtil;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

public final class CeroneDefinitions {

  public static final ExecutionFormula<CeroneExecution> EXT =
      e -> {
        Variable T = Variable.unary("T");
        Variable x = Variable.unary("x");
        Variable n = Variable.unary("n");

        Expression wrote_to_x_and_was_seen_by_t =
            e.vis().join(T).intersection(e.history().txnThatWriteToAnyOf(x));
        Formula first_read =
            wrote_to_x_and_was_seen_by_t.no().and(n.eq(e.history().initialValue(x)));
        Expression last_txn_to_write_x = KodkodUtil.max(e.ar(), wrote_to_x_and_was_seen_by_t);
        Formula last_write_to_x_was_n =
            wrote_to_x_and_was_seen_by_t
                .some()
                .and(
                    e.history()
                        .finalWrite(
                            last_txn_to_write_x,
                            x,
                            n)); // guarantees max is only used with a non-empty set

        return (e.history().externalRead(T, x, n).implies(first_read.or(last_write_to_x_was_n)))
            .forAll(
                T.oneOf(e.history().transactions())
                    .and(x.oneOf(e.history().keys()))
                    .and(n.oneOf(e.history().values())));
      };

  public static final ExecutionFormula<CeroneExecution> NO_CONF =
      e -> {
        Variable T = Variable.unary("T");
        Variable S = Variable.unary("S");
        Variable x = Variable.unary("x");

        Formula distinct_transactions = S.eq(T).not();
        Formula both_write_x = e.history().writes(T, x).and(e.history().writes(S, x));
        Formula one_sees_the_other = (T.product(S)).in(e.vis()).or((S.product(T)).in(e.vis()));

        return distinct_transactions
            .and(both_write_x)
            .implies(one_sees_the_other)
            .forAll(
                T.oneOf(e.history().transactions())
                    .and(S.oneOf(e.history().transactions()))
                    .and(x.oneOf(e.history().keys())));
      };

  public static final ExecutionFormula<CeroneExecution> TRANS_VIS =
      e -> KodkodUtil.transitive(e.vis());

  public static final ExecutionFormula<CeroneExecution> PREFIX =
      e -> e.ar().join(e.vis()).in(e.vis());

  public static final ExecutionFormula<CeroneExecution> TOTAL_VIS =
      e -> KodkodUtil.total(e.vis(), e.history().transactions());

  public static final ExecutionFormula<CeroneExecution> SESSION =
      e -> e.history().sessionOrder().in(e.vis());

  public static final ExecutionFormula<CeroneExecution> RA = e -> Formula.TRUE;
  public static final ExecutionFormula<CeroneExecution> CC = TRANS_VIS;
  public static final ExecutionFormula<CeroneExecution> UA = NO_CONF;
  public static final ExecutionFormula<CeroneExecution> PSI = TRANS_VIS.and(NO_CONF);
  public static final ExecutionFormula<CeroneExecution> PC = PREFIX;
  public static final ExecutionFormula<CeroneExecution> SI = PREFIX.and(NO_CONF);
  public static final ExecutionFormula<CeroneExecution> Ser = TOTAL_VIS;
}
