package haslab.isolde.core.synth;

import kodkod.instance.TupleSet;

public record TransactionTotalOrderInfo(boolean usable, TupleSet txnTotalOrder) {}
