package haslab.isolde.experiments;

import java.util.Date;

public record Measurement(
    String implementation,
    String solver,
    String satisfied,
    String violated,
    Integer num_txn,
    Integer num_keys,
    Integer num_values,
    Integer num_sessions,
    Long time_ms,
    Integer candidates,
    Date date) {}
