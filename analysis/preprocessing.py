from types import NoneType
from typing import List, Tuple

from config import sat
from domain import Problem, Solver, Framework

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
    txn_num: NoneType | Tuple[int, int] | List[int] = None,
    remove_timeouts=False,
    solvers=None,
    problems=None,
    implementations=None,
):
    df = typify(df)
    df = trim(
        df,
        txn_num=txn_num,
        remove_timeouts=remove_timeouts,
        solvers=solvers,
        problems=problems,
        implementations=implementations,
    )
    df = merge_rows(df, check_num_measurements=False)
    df['timeout'] = df['outcome'].apply(lambda o: o == 'TIMEOUT')
    df['crash'] = df['outcome'].apply(lambda o: o == 'CRASH')

    # remove rows that have "not RA_c" in them
    def has_RA_c(row):
        problem = row['problem']
        return problem.neg.name == "RA" and problem.neg.framework == Framework.CERONE

    df = df.loc[df.apply(lambda row: not has_RA_c(row), axis=1)]
    return df



# Validates a dataframe. A dataframe is valid iff:
# - all different setups (i.e., implementation + solver + input problem + scope) have the same number of measurements
# - each setup always results in the same number of candidates
def validate(df, setup=setup, check_num_measurements=False, check_expected=False):

    if check_expected:
        # Assert that the result of all non-timeout rows is equivalent to the expected result
        if not ((df["outcome"] == "TIMEOUT") | (df["outcome"] == df["expected"])).all():
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


def trim(
    df,
    txn_num: NoneType | Tuple[int, int] | List[int] = None,
    remove_timeouts=False,
    solvers=None,
    problems=None,
    implementations=None,
):
    if txn_num != None:
        if isinstance(txn_num, Tuple):
            txn_num = list(range(txn_num[0], txn_num[1] + 1))
        df = df[df["num_txn"].isin(txn_num)]
    if remove_timeouts:
        df = df[~(df["outcome"] == "TIMEOUT")]
    if solvers:
        df = df[df["solver"].isin(solvers)]
    if problems:
        df = df[df["problem"].isin(problems)]
    if implementations:
        df = df[df["implementation"].isin(implementations)]
    return df


def typify(df):
    df["problem"] = df["problem"].apply(Problem.from_str)
    df["solver"] = df["solver"].apply(lambda s: Solver(s.lower()))
    df["expected"] = df["problem"].apply(lambda p: "SAT" if sat(p) else "UNSAT")
    return df


# Cleans a dataframe by grouping together rows corresponding to the same setup,
# creating three new columns "avg_time_ms", "max_time_ms", and "min_time_ms".
# We keep the number of candidates of each group.
def merge_rows(
    df,
    setup=setup,
    check_num_measurements=True,
):
    validation_result = validate(df, setup, check_num_measurements)
    assert validation_result[0], validation_result[1]
    assert df['num_sessions'].nunique() == 1, f"Expected all rows to have the same number of sessions, instead there are {df['num_sessions'].unique()}"
    df = df.drop(columns=['num_sessions'])
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
                lambda x: "TIMEOUT" if (x == "TIMEOUT").any() else ("CRASH" if (x == 'CRASH').any() else x.iloc[0]),
            ),
            expected=("expected", "first"),
        )
        .reset_index()
    )
    df["avg_time_ms"] = df["avg_time_ms"].round().astype(int)
    df["frameworks"] = df["problem"].apply(lambda p: tuple(p.frameworks()))
    df["terminates"] = df["outcome"].apply(lambda o: o != 'TIMEOUT' and o != 'CRASH')
    df["problem_type"] = df.apply(
        lambda row: (row["expected"], len(row["frameworks"])), axis=1
    )
    return df
