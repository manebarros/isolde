# Validates a df. Tests if:
# - all different setups (i.e., implementation + solver + input problem + scope) have the same number of measurements
# - each setup always results in the same number of candidates
def validate(df, setup, check_num_measurements=True):
    # Assert unique values for the number of keys, values, and sessions
    params = ["num_keys", "num_values", "num_sessions"]
    for param in params:
        values = df[param].unique()
        if len(values) > 1:
            return (
                False,
                f"Not all measurements use the same '{param}' value. Values present: {values}",
            )

    grouped = df.groupby(setup)

    # Assert that all setups have the same number of measurements
    if check_num_measurements and grouped.size().nunique() != 1:
        number_of_measurements = grouped.size().unique()
        return (
            False,
            f"Not all setups have the same number of measurements. Numbers of measurements: {number_of_measurements}",
        )

    # Assert that among a group we have a single number of candidates
    if not (grouped["candidates"].nunique() == 1).all():
        return (False, f"Not all groups have a consistent 'candidates' value")
    return (True, None)


# Cleans a dataframe by grouping together rows corresponding to the same setup,
# creating three new columns "avg_time_ms", "max_time_ms", and "min_time_ms".
# We keep the number of candidates of each group.
def clean(
    df,
    setup=setup,
    txn_max_lim=None,
    check_num_measurements=True,
    remove_timeouts=False,
):
    validation_result = validate(df, setup, check_num_measurements)
    assert validation_result[0], validation_result[1]
    if txn_max_lim:
        df = df[df["num_txn"] <= txn_max_lim]
    if remove_timeouts:
        df = df[df["timed_out"] == False]
    return (
        df.groupby(setup)
        .agg(
            candidates=("candidates", "first"),
            avg_time_ms=("time_ms", "mean"),
            min_time_ms=("time_ms", "min"),
            max_time_ms=("time_ms", "max"),
        )
        .reset_index()
    )
