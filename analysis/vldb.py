import argparse
import os
import time
from pathlib import Path
from sys import implementation

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import preprocessing as pre
from domain import Framework, Problem
from pandas.core import sorting
from plotting import Style, plot

Class = tuple[str, int]

TableData = dict[str, dict[str, dict[int, tuple[tuple[int, int], tuple[int, int]]]]]
Helper = dict[str, dict[str, int]]

TIMEOUT = 3600000

STYLES = {
    "all": Style("Isolde", "#E69F00", "D", "-."),  # orange-yellow
    "no_smart_search": Style("No smart search", "#56B4E9", "v", "--"),  # sky blue
    "no_fixed_co": Style("No fixed co", "#009E73", "P", "-"),
    "no_learning": Style("No learning", "#CC79A7", "X", ":"),
    "no_incremental": Style("No incremental", "#CC79A7", "X", ":"),
    "brute_force": Style("Brute force", "#CC79A8", "X", "--"),
}


def dfToTableData(df, txn_lim=10) -> TableData:
    d = {}
    helper = {}
    for _, row in df.iterrows():
        implementation = row["implementation"]
        num_txn = row["num_txn"]
        expected = row["expected"]
        avg_time_ms = row["avg_time_ms"]
        frameworks = row["frameworks"]
        candidates = row["candidates"]

        if implementation not in d:
            d[implementation] = {}
            helper[implementation] = {}
        problem_class = (expected, frameworks)
        if problem_class not in d[implementation]:
            d[implementation][problem_class] = {}
        if num_txn not in d[implementation][problem_class]:
            d[implementation][problem_class][num_txn] = (
                (avg_time_ms, candidates),
                (avg_time_ms, candidates),
            )

        (min_time, min_cand), (max_time, max_cand) = d[implementation][problem_class][
            num_txn
        ]

        if avg_time_ms < min_time:
            min_time, min_cand = avg_time_ms, candidates
        if avg_time_ms > max_time:
            max_time, max_cand = avg_time_ms, candidates

        d[implementation][problem_class][num_txn] = (
            (min_time, min_cand),
            (max_time, max_cand),
        )

        if avg_time_ms == TIMEOUT:
            for i in range(num_txn + 1, txn_lim + 1):
                if i in d[implementation][problem_class]:
                    min_time = d[implementation][problem_class][i][0]
                else:
                    min_time = (TIMEOUT + 1000, -1)
                d[implementation][problem_class][i] = (min_time, (avg_time_ms, -1))
    return d


def formatTable2(data: TableData, num_txn=5) -> str:
    s = ""
    for impl, impl_data in data.items():
        s += r"\midrule" + "\n" + r"\multirow{2}{*}{" + impl.replace("_", " ") + r"}"
        for pclass, pclass_data in sorted(impl_data.items()):
            (_, (time, _)) = pclass_data[num_txn]
            s += f" & {time if time < TIMEOUT else "\\multirow{2}{*}{TO}"}"
        s += r"\\" + "\n\n"
        for pclass, pclass_data in sorted(impl_data.items()):
            (_, (time, cand)) = pclass_data[num_txn]
            s += f" & {cand if time < TIMEOUT else ""}"
        s += r"\\" + "\n\n"
    return s


table2Header = r"""\toprule
                    & SAT single fw & SAT diff fws & UNSAT single fw & UNSAT diff fws \\
"""

def formatTable3(data: TableData) -> str:
    s = ""
    for impl, impl_data in data.items():
        s += r"\midrule" + "\n" + r"\multirow{2}{*}{" + impl.replace("_", " ") + r"}"
        for pclass, pclass_data in sorted(impl_data.items()):
            for num_txn, (_, (time, cand)) in pclass_data.items():
                s += f" & {time if time < TIMEOUT else "\\multirow{2}{*}{TO}"}"
        s += r"\\" + "\n"
        for pclass, pclass_data in sorted(impl_data.items()):
            for num_txn, (_, (time, cand)) in pclass_data.items():
                s += f" & {cand if time < TIMEOUT else ""}"
        s += r"\\" + "\n\n"
    return s


table3Header = r"""\toprule
                    & \multicolumn{3}{c}{SAT single fw} & \multicolumn{3}{c}{SAT diff fws} & \multicolumn{3}{c}{UNSAT single fw} & \multicolumn{3}{c}{UNSAT diff fws} \\
		\cmidrule(lr){2-13}
                    & 3 & 4 & 5 & 3 & 4 & 5 & 3 & 4 & 5  & 3 & 4 & 5 \\  
"""


def prepare(path):
    df = pd.read_csv(path)
    df = pre.preprocess(df, txn_num=(3, 10))
    df["timeout"] = df["outcome"].apply(lambda o: o == "TIMEOUT")

    # remove rows that have not RA_c in them
    def has_RA_c(row):
        problem = row["problem"]
        return problem.neg.name == "RA" and problem.neg.framework == Framework.CERONE

    df = df.loc[df.apply(lambda row: not has_RA_c(row), axis=1)]
    return df


def plot1(df, basedir=None):

    df = df.copy(deep=True)
    df = df[df["implementation"] != "no_incremental"]

    # Group and count non-timeout rows
    filtered = df[df["timeout"] == False]

    # Build the full combination grid
    full_index = pd.MultiIndex.from_product(
        [
            df["problem_type"].unique(),
            df["num_txn"].unique(),
            df["implementation"].unique(),
        ],
        names=["problem_type", "num_txn", "implementation"],
    )

    # Reindex and fill missing with 0
    grouped = (
        filtered.groupby(["problem_type", "num_txn", "implementation"])
        .size()
        .reindex(full_index, fill_value=0)
        .reset_index(name="count")
    )

    problem_types = grouped["problem_type"].unique()
    implementations = list(grouped["implementation"].unique())
    implementations.append("brute_force")

    fig, axes = plt.subplots(
        1, len(problem_types), figsize=(3 * len(problem_types), 3), sharey=True
    )

    # Handle case of single problem type
    if len(problem_types) == 1:
        axes = [axes]

    for idx, (ax, problem) in enumerate(zip(axes, sorted(problem_types))):
        subset = grouped[grouped["problem_type"] == problem]

        offsets = {impl: i * 0.08 for i, impl in enumerate(implementations)}

        for impl in implementations:
            if impl != "brute_force":
                impl_data = subset[subset["implementation"] == impl].sort_values(
                    "num_txn"
                )
                x = impl_data["num_txn"] + offsets[impl]
                ax.plot(
                    x,
                    impl_data["count"],
                    marker=STYLES[impl].marker,
                    color=STYLES[impl].color,
                    label=STYLES[impl].name,
                    linestyle=STYLES[impl].linestyle,
                )
            else:
                indexes = [i + offsets[impl] for i in list(range(3, 11))]
                ax.plot(
                    indexes,
                    list([0] * len(indexes)),
                    label=STYLES[impl].name,
                    marker=STYLES[impl].marker,
                    color=STYLES[impl].color,
                    linestyle=STYLES[impl].linestyle,
                )

        ax.set_title(problem)
        ax.set_ylim(bottom=-0.5, top=15)
        ax.grid()
        if idx == 0:
            ax.legend(loc="best")
            ax.set_xlabel("Number of transactions")
            ax.set_ylabel("Number of finished runs")

    plt.tight_layout()

    if not basedir:
        plt.show()
    else:
        plt.savefig(os.path.join(basedir, "plot1.pgf"))
        plt.savefig(os.path.join(basedir, "plot1.pdf"))


def plot2(df, basedir=None):
    problems = [
        Problem.from_str("SI_ax_c UpdateSer__c\tSer_ax_c"),
        Problem.from_str("SI_ax_c UpdateSer__c\tSer_ax_b"),
        Problem.from_str("RA_ax_c\tCC_ax_c"),
    ]
    local = df[df["implementation"].isin(["all", "no_learning"])]
    local = local[local["problem"].isin(problems)]

    styles = {
        "all": Style("Isolde", "#E69F00", "D", "-."),
        "no_learning": Style("Baseline", "#CC79A7", "X", ":"),
    }

    paths = ["plot2.pgf", "plot2.pdf"] if basedir else None

    plot(
        local,
        "problem",
        "solver",
        "implementation",
        paths=paths,
        base_dir=basedir,
        styles=styles,
        logScaling=True,
        plotHeight=4,
        plotWidth=6,
        legend=True,
        col_display_fun=lambda p: p.as_latex(),
        y_unit="ms",
        sharey=False,
    )


def plot3(df, basedir=None):

    problems = [
        Problem.from_str("PC_ax_b\tSI_ax_c"),
        Problem.from_str("PC_ax_c\tCC_ax_c"),
        Problem.from_str("Ser_ax_c\tSer_ax_b"),
        Problem.from_str("CC_ax_b\tCC_ax_c"),
    ]

    local = df[df["implementation"].isin(["all", "no_smart_search", "no_fixed_co"])]
    local = local[local["problem"].isin(problems)]

    sorting_keys = {val: idx for idx, val in enumerate(problems)}

    paths = ["plot3.pgf", "plot3.pdf"] if basedir else None

    plot(
        local,
        "problem",
        "solver",
        "implementation",
        paths=paths,
        base_dir=basedir,
        styles=STYLES,
        logScaling=True,
        plotHeight=4,
        plotWidth=5,
        legend=True,
        col_display_fun=lambda p: p.as_latex(),
        y_unit="ms",
        sharey=False,
        timeout=3600000,
        col_order=sorting_keys,
    )

# For every problem `p` in the df, if there is some row `r` such that `problem(r) == p` and
# `outcome(r) == TIMEOUT`, remove all rows in df that have `p` as their problem.
def exclude_problems_that_timeout(df):
    problems_to_remove = df.loc[df['outcome'] == 'TIMEOUT', 'problem'].unique()
    df = df[~df['problem'].isin(problems_to_remove)]
    return df

def compute_means(df):
    grouping_cols = [
        "problem_type",
        "implementation",
        "solver",
        "num_txn",
        "num_keys",
        "num_values",
    ]

    df = (
        df.groupby(grouping_cols)
        .agg(
            avg_cand=("candidates", "mean"),
            min_cand=("candidates", "min"),
            max_cand=("candidates", "max"),
            avg_time_ms=("avg_time_ms", "mean"),
            min_time_ms=("min_time_ms", "min"),
            max_time_ms=("max_time_ms", "max"),
        )
        .reset_index()
    )
    df["avg_time_ms"] = df["avg_time_ms"].round().astype(int)
    return df

def fill_with_timeouts(df, txn_lim=10):
    default_vals = {
        'avg_time_ms' : TIMEOUT,
        'min_time_ms' : TIMEOUT,
        'max_time_ms' : TIMEOUT,
    }

    extra_feats = [ 
        "outcome", 	
        "expected", 	
        "frameworks", 	
        "problem_type", 	
        "timeout", 	
        "crash"
    ]

    new_rows = []

    for _, r in df.iterrows():
        if r['timeout']:
            n = r['num_txn']
            for k in range(n + 1, txn_lim + 1):  # n+1 up to and including 10
                new_row = {}
                # copy setup from r
                for col in pre.setup:
                    new_row[col] = r[col]
                for col in extra_feats:
                    new_row[col] = r[col]
                # override num_txn
                new_row['num_txn'] = k
                # fill remaining cols with defaults
                for col, val in default_vals.items():
                    new_row[col] = val
                new_rows.append(new_row)

    df = pd.concat([df, pd.DataFrame(new_rows)], ignore_index=True)
    return df

def double_timeout(df):
    df_copy = df.copy()
    mask = df_copy['timeout'] == True
    df_copy.loc[mask, ['x', 'y', 'z']] *= 2
    return df_copy

def cactus_plot(df, num_txns=None):
    # Get unique values for subplot dimensions
    problem_types = sorted(df["problem_type"].unique())
    if not num_txns:
        num_txns = sorted(df["num_txn"].unique())
    implementations = df["implementation"].unique()

    n_cols = len(problem_types)
    n_rows = len(num_txns)

    fig, axes = plt.subplots(
        n_rows, n_cols, figsize=(5 * n_cols, 4 * n_rows), squeeze=False
    )

    for r, txn in enumerate(num_txns):
        for c, prob in enumerate(problem_types):
            ax = axes[r][c]
            subset = df[(df["problem_type"] == prob) & (df["num_txn"] == txn)]

            for impl in implementations:
                impl_data = subset[subset["implementation"] == impl][
                    "max_time_ms"
                ].dropna()
                if impl_data.empty:
                    continue

                # Sort runtimes and compute cumulative count
                sorted_times = np.sort(impl_data.values)
                cumulative = np.arange(1, len(sorted_times) + 1)

                # Extend line to make it a step function starting from 0
                sorted_times = np.concatenate([[0], sorted_times])
                cumulative = np.concatenate([[0], cumulative])

                ax.step(sorted_times, cumulative, where="post", label=impl)

            ax.set_title(f"{prob} | txn={txn}")
            ax.set_xlabel("Runtime")
            ax.set_ylabel("Accumulated runs")
            ax.legend(fontsize="small")

    plt.tight_layout()
    plt.show()


DATA_FILE = "/home/mane/code/isolde/isolde-experiments/data/d8dfd9f4814950.csv"


def main():
    parser = argparse.ArgumentParser(description="Plot builder")
    parser.add_argument("path", nargs="?", default=None, help="Path to the data file")
    parser.add_argument("--dest", "-d", default="./plots", help="Dest directory")
    args = parser.parse_args()

    path = args.path if args.path else DATA_FILE
    commit_id = Path(path).stem
    dir = os.path.join(args.dest, commit_id)
    Path(dir).mkdir(exist_ok=True, parents=True)

    df = prepare(path)

    plot1(df, dir)
    plot2(df, dir)
    plot3(df, dir)


if __name__ == "__main__":
    main()
