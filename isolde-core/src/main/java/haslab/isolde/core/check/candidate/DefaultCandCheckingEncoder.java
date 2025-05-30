package haslab.isolde.core.check.candidate;

import static haslab.isolde.core.DirectAbstractHistoryEncoding.initialTransaction;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.keys;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.reads;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.sessionOrder;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.sessions;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.transactions;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.txn_session;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.values;
import static haslab.isolde.core.DirectAbstractHistoryEncoding.writes;
import static haslab.isolde.kodkod.KodkodUtil.asTupleSet;
import static haslab.isolde.kodkod.Util.unaryTupleSetToAtoms;

import haslab.isolde.core.AbstractHistoryK;
import haslab.isolde.core.AbstractHistoryRel;
import haslab.isolde.core.DirectAbstractHistoryEncoding;
import haslab.isolde.core.general.simple.HistoryEncoderS;
import haslab.isolde.kodkod.Util;
import kodkod.ast.Formula;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;

public class DefaultCandCheckingEncoder implements HistoryEncoderS<ContextualizedInstance> {

  private DefaultCandCheckingEncoder() {}

  private static DefaultCandCheckingEncoder instance = null;

  public static DefaultCandCheckingEncoder instance() {
    if (instance == null) {
      instance = new DefaultCandCheckingEncoder();
    }
    return instance;
  }

  @Override
  public AbstractHistoryRel encoding() {
    return DirectAbstractHistoryEncoding.instance();
  }

  @Override
  public Formula encode(ContextualizedInstance contextualizedInstance, Bounds b) {
    Instance instance = contextualizedInstance.instance();
    AbstractHistoryK context = contextualizedInstance.context();
    Evaluator ev = new Evaluator(instance);
    TupleFactory f = b.universe().factory();
    b.boundExactly(
        transactions, asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.transactions()))));
    b.boundExactly(keys, asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.keys()))));
    b.boundExactly(values, asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.values()))));
    b.boundExactly(sessions, asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.sessions()))));
    b.boundExactly(
        initialTransaction,
        asTupleSet(f, unaryTupleSetToAtoms(ev.evaluate(context.initialTransaction()))));

    b.boundExactly(writes, Util.convert(ev, context, AbstractHistoryK::finalWrites, f, 3));
    b.boundExactly(reads, Util.convert(ev, context, AbstractHistoryK::externalReads, f, 3));
    b.boundExactly(sessionOrder, Util.convert(ev, context, AbstractHistoryK::sessionOrder, f, 2));
    b.boundExactly(txn_session, Util.convert(ev, context, AbstractHistoryK::txn_session, f, 2));

    return Formula.TRUE;
  }
}
