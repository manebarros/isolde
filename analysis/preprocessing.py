from config import sat
from domain import Framework, Problem, Solver

# These columns identify a particular configuration for an Isolde run
setup = [
    "problem",
    "implementation",
    "solver",
    "num_txn",
    "num_keys",
    "num_values",
]


def preprocess(
    df,
    setup=setup,
    check_num_measurements=False,
    check_expected=False,
):
    df["problem"] = df["problem"].apply(Problem.from_str)
    df["solver"] = df["solver"].apply(lambda s: Solver(s.lower()))
    df["expected"] = df["problem"].apply(lambda p: "SAT" if sat(p) else "UNSAT")

    validation_result = validate(
        df,
        setup,
        check_num_measurements=check_num_measurements,
        check_expected=check_expected,
    )
    assert validation_result[0], validation_result[1]

    if "num_sessions" in df:
        assert (
            df["num_sessions"].nunique() == 1
        ), f"Expected all rows to have the same number of sessions, instead there are {df['num_sessions'].unique()}"
        df = df.drop(columns=["num_sessions"])

    df = (
        df.groupby(setup)
        .agg(
            candidates=("candidates", "first"),
            initial_clauses=("initial_synth_clauses", "first"),
            final_clauses=("total_synth_clauses", "first"),
            avg_time_ms=("total_time_ms", "mean"),
            min_time_ms=("total_time_ms", "min"),
            max_time_ms=("total_time_ms", "max"),
            outcome=(
                "outcome",
                lambda x: (
                    "TIMEOUT"
                    if (x == "TIMEOUT").any()
                    else ("CRASH" if (x == "CRASH").any() else x.iloc[0])
                ),
            ),
            expected=("expected", "first"),
        )
        .reset_index()
    )
    df["avg_time_ms"] = df["avg_time_ms"].round().astype(int)
    df["frameworks"] = df["problem"].apply(lambda p: tuple(p.frameworks()))
    df["terminates"] = df["outcome"].apply(lambda o: o != "TIMEOUT" and o != "CRASH")
    df["problem_type"] = df.apply(
        lambda row: (row["expected"], len(row["frameworks"])), axis=1
    )

    # remove rows that have "not RA_c" in them
    def has_RA_c(row):
        problem = row["problem"]
        return problem.neg.name == "RA" and problem.neg.framework == Framework.CERONE

    df = df.loc[df.apply(lambda row: not has_RA_c(row), axis=1)]
    df = df[
        (df["implementation"] != "no_smart_search")
        | (df["problem_type"] != ("UNSAT", 2))
    ]
    return df


# Validates a dataframe. A dataframe is valid iff:
# - all different setups (i.e., implementation + solver + input problem + scope) have the same number of measurements
# - each setup always results in the same number of candidates
def validate(df, setup=setup, check_num_measurements=False, check_expected=False):

    if check_expected:
        # Assert that the result of all non-timeout rows is equivalent to the expected result
        if not (
            (df["outcome"].isin(["TIMEOUT", "CRASH"]))
            | (df["outcome"] == df["expected"])
        ).all():
            return (False, f"Results do not match expected results.")

    grouped = df.groupby(setup)

    # Assert that all setups have the same number of measurements
    if check_num_measurements and grouped.size().nunique() != 1:
        number_of_measurements = grouped.size().unique()
        return (
            False,
            f"Not all setups have the same number of measurements. Numbers of measurements: {number_of_measurements}",
        )

    # Assert determinism in the fields 'candidates', 'initial_synth_clauses', and 'total_synth_clauses'
    fields = ["candidates", "initial_synth_clauses", "total_synth_clauses", "outcome"]
    for field in fields:
        if not (grouped[field].nunique() == 1).all():
            return (False, f"Not all groups have a consistent '{field}' value")

    return (True, None)
