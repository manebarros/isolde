import os
from enum import Enum
from typing import List

import matplotlib.pyplot as plt
import numpy as np
from matplotlib.ticker import FormatStrFormatter

BLUE = "#1f77b4"
ORANGE = "#ff7f0e"
GREEN = "#2ca02c"
PURPLE = "#800080"


class Style:
    def __init__(self, name, color, marker, linestyle):
        self.name = name
        self.color = color
        self.marker = marker
        self.linestyle = linestyle


solver_display_names = {"minisat": "MiniSat", "glucose": "Glucose", "sat4j": "Sat4j"}

implementation_styles = {
    "default": Style("Optimized", "#E69F00", "D", "-."),  # orange-yellow
    "no optimizations": Style("No optimization", "#56B4E9", "v", "--"),  # sky blue
    "without fixed order": Style("No fixed txn order", "#009E73", "P", "-"),
    "without smart search": Style("No smart search", "#CC79A7", "X", ":"),
}

# These columns identify a particular configuration for an Isolde run
setup = [
    "implementation",
    "solver",
    "problem",
    "num_txn",
    "num_keys",
    "num_values",
    "num_sessions",
]


class Framework(Enum):
    B = "b"
    C = "c"


class Definition:
    def __init__(self, name: str, fw: str) -> None:
        self.name = name
        self.fw = Framework(fw)


class Problem:
    def __init__(self, pos: List[Definition], neg: Definition) -> None:
        self.pos = pos
        self.neg = neg


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


def level_name_as_latex_without_framework(level_name):
    (level, _) = level_name.split("_")
    return level


def framework(level):
    return parse_level(level)[1]


def parse_level(level):
    (level, fw) = level.split("_")
    return (level, fw)


def parse(problem):
    (pos, neg) = problem.split("\t")
    pos_lst = pos.split(" ")
    return list(map(parse_level, pos_lst)), parse_level(neg)


def parse_and_show(problem: str) -> str:
    return problem


def problem_as_latex(problem_str, use_dollar_sign=True, simple=False):
    (pos_lst, neg) = parse(problem_str)
    pos_str = intersperse([level_as_latex(l, simple=simple) for l in pos_lst])
    neg_str = level_as_latex(neg, simple=simple)
    if use_dollar_sign:
        return rf"$\{{{pos_str}, \, \overline{{{neg_str}}}\}}$"
    else:
        return rf"\(\{{{pos_str}, \, \overline{{{neg_str}}}\}}\)"


def frameworks(problem: str) -> List[str]:
    (pos, neg) = parse(problem)
    frameworks = {fw for (_, fw) in pos}
    frameworks.add(neg[1])
    frameworks = list(frameworks)
    frameworks.sort()
    return frameworks


def showframeworks(frameworks: List[str]) -> str:
    s = showFramework(frameworks[0])
    for fw in frameworks[1:]:
        s = s + r",\ " + showFramework(fw)
    return s


def intersperse(l: List[str]) -> str:
    s = l[0]
    for fw in l[1:]:
        s = s + r",\ " + fw
    return s


def showFramework(fw):
    return r"\mathcal{B}" if fw == "b" else r"\mathcal{C}"


def level_name_as_latex(level_name):
    (level, fw) = level_name.split("_")
    fw = r"\mathcal{B}" if fw == "b" else r"\mathcal{C}"
    return level + r"_{" + fw + r"}"


def level_as_latex(level: tuple[str, str], simple=False) -> str:
    (level_name, fw) = level
    if level_name == "UpdateSer":
        level_name = "US"
    if level_name == "PlumeRA":
        level_name = "TapRA"
    if level_name == "PlumeCC":
        level_name = "TapCC"
    fw = r"\mathcal{B}" if fw == "b" else r"\mathcal{C}"
    if simple:
        return rf"{level_name}" + r"_{" + fw + r"}"
    return rf"\textsc{{{level_name}}}" + r"_{" + fw + r"}"


def determine_unit(max_val):
    if max_val < 400:
        return 1, "ms"
    else:
        return 1000, "s"


def get_formatter(scale_factor):
    # return lambda v, _: f"{v / scale_factor:.1f}"
    return lambda v, _: f"{int(v / scale_factor)}"


# Given a cleaned dataframe, draw a matrix of plots for the given `specs`.
# Specs is a list of pairs (satisfied, violated)
def plot_specs(
    df,
    specs,
    logScaling=False,
    plotHeight=4.0,
    plotWidth=5.0,
    paths=[],
    base_dir=None,
    display_level_fun=level_name_as_latex,
    implementations=["optimized"],
):
    # The number of solvers
    col_keys = df["solver"].unique()

    # Create subplot grid
    n_rows = len(specs)
    n_cols = len(col_keys)

    fig, axes = plt.subplots(
        n_rows,
        n_cols,
        figsize=(plotWidth * n_cols, plotHeight * n_rows),
        sharex=True,
        sharey=False,
    )

    # If only one row or column, make axes 2D
    if n_rows == 1:
        axes = axes[np.newaxis, :]
    if n_cols == 1:
        axes = axes[:, np.newaxis]

    # Plot each subplot
    for i, (satisfied, violated) in enumerate(specs):

        # We calculate the maximum y-limit for this row
        max_y = df[(df["satisfied"] == satisfied) & (df["violated"] == violated)][
            "max_time_ms"
        ].max()

        min_y = df[(df["satisfied"] == satisfied) & (df["violated"] == violated)][
            "min_time_ms"
        ].min()

        # Use `y_lim` to infer the correct unit to use
        scale_factor, unit = determine_unit(max_y)

        for j, solver in enumerate(col_keys):
            ax = axes[i, j]

            for implementation in implementations:
                subset = df[
                    (df["satisfied"] == satisfied)
                    & (df["violated"] == violated)
                    & (df["solver"] == solver)
                    & (df["implementation"] == implementation)
                ]
                ax.errorbar(
                    subset["num_txn"],
                    subset["avg_time_ms"],
                    yerr=[
                        subset["avg_time_ms"] - subset["min_time_ms"],
                        subset["max_time_ms"] - subset["avg_time_ms"],
                    ],
                    capsize=3,
                    markersize=5,
                    label=(
                        implementation_styles[implementation].name
                        if len(implementations) > 1
                        else None
                    ),
                    color=implementation_styles[implementation].color,
                    marker=implementation_styles[implementation].marker,
                    linestyle=implementation_styles[implementation].linestyle,
                )

            if logScaling == True or (
                logScaling != False and (satisfied, violated) in logScaling
            ):
                ax.set_yscale("log")
                ax.set_ylim(bottom=0.95 * min_y, top=1.15 * max_y)
            else:
                ax.set_ylim(bottom=0, top=1.05 * max_y)

            # Use an appropriate unit for they y-axis
            ax.yaxis.set_major_formatter(get_formatter(scale_factor))

            # Only the top row gets titles
            if i == 0:
                ax.set_title(solver_display_names[solver])

            # Only the left-most column gets y-axis labels
            if j == 0:
                ax.set_ylabel(
                    r"$\left\{"
                    + display_level_fun(satisfied)
                    + r", \, \overline{"
                    + display_level_fun(violated)
                    + r"}\right\}$"
                    + "\n\nTime "
                    + f"({unit})"
                )
            else:
                ax.set_ylabel("")
                ax.set_yticklabels([])

            # Only the bottom-left gets x-axis label
            if i == n_rows - 1 and j == n_cols // 2:
                ax.set_xlabel("Number of Transactions")
            else:
                ax.set_xlabel("")

    plt.tight_layout()
    for path in paths:
        if base_dir:
            plt.savefig(os.path.join(base_dir, path))
        else:
            plt.savefig(path)


# Given a cleaned dataframe, draw a matrix of plots for the given `specs`.
# Specs is a list of pairs (satisfied, violated)
def plot_problems(
    df,
    problems,
    implementations=None,
    logScaling=False,
    plotHeight=4.0,
    plotWidth=5,
    paths=[],
    base_dir=None,
    display_level_fun=None,
    legend=False,
    unit="auto",
):
    assert unit in ["auto", "s", "ms"]

    # The number of solvers
    col_keys = df["solver"].unique()

    if implementations == None:
        implementations = df["implementation"].unique()

    # Create subplot grid
    n_rows = len(problems)
    n_cols = len(col_keys)

    print(n_rows)
    print(n_cols)

    fig, axes = plt.subplots(
        n_rows,
        n_cols,
        figsize=(plotWidth * n_cols, plotHeight * n_rows),
        sharex=True,
        sharey=False,
    )

    # Make axes always a 2D array for consistent indexing
    if n_rows == 1 and n_cols == 1:
        axes = np.array([[axes]])
    elif n_rows == 1:
        axes = axes[np.newaxis, :]
    elif n_cols == 1:
        axes = axes[:, np.newaxis]

    # Plot each subplot
    for i, problem in enumerate(problems):

        # We calculate the maximum y-limit for this row
        max_y = df[(df["problem"] == problem)]["max_time_ms"].max()

        min_y = df[(df["problem"] == problem)]["min_time_ms"].min()

        if unit == "auto":
            # Use `y_lim` to infer the correct unit to use
            scale_factor, unit = determine_unit(max_y)
        elif unit == "s":
            scale_factor, unit = 1000, "s"
        else:
            scale_factor, unit = 1, "ms"

        for j, solver in enumerate(col_keys):
            ax = axes[i, j]

            for implementation in implementations:
                subset = df[
                    (df["problem"] == problem)
                    & (df["solver"] == solver)
                    & (df["implementation"] == implementation)
                ]
                ax.errorbar(
                    subset["num_txn"],
                    subset["avg_time_ms"],
                    yerr=[
                        subset["avg_time_ms"] - subset["min_time_ms"],
                        subset["max_time_ms"] - subset["avg_time_ms"],
                    ],
                    capsize=3,
                    markersize=5,
                    label=(
                        implementation_styles[implementation].name
                        if len(implementations) > 1
                        else None
                    ),
                    color=implementation_styles[implementation].color,
                    marker=implementation_styles[implementation].marker,
                    linestyle=implementation_styles[implementation].linestyle,
                )

            if logScaling == True or (logScaling != False and problem in logScaling):
                ax.set_yscale("log")
                ax.set_ylim(bottom=0.95 * min_y, top=1.15 * max_y)
            else:
                print(problem)
                print(max_y)
                ax.set_ylim(bottom=0, top=1.05 * max_y)

            # Use an appropriate unit for they y-axis
            ax.yaxis.set_major_formatter(get_formatter(scale_factor))

            # Only the top row gets titles
            if i == 0 and n_cols > 1:
                ax.set_title(solver_display_names[solver])

            # Only the left-most column gets y-axis labels
            if j == 0:
                label = f"Time ({unit})"
                if display_level_fun != None:
                    label = display_level_fun(problem) + "\n\n" + label
                ax.set_ylabel(label)
            else:
                ax.set_ylabel("")
                ax.set_yticklabels([])

            # Only the bottom-left gets x-axis label
            if i == n_rows - 1 and j == n_cols // 2:
                ax.set_xlabel("Number of Transactions")
            else:
                ax.set_xlabel("")

            # Only the top-left gets legend
            if legend and i == 0 and j == 0:
                ax.legend(loc="upper left")

            ax.grid(
                True,  # turn grid on
                which="both",  # 'major', 'minor', or 'both'
                axis="both",  # 'x', 'y', or 'both'
                linestyle="--",  # e.g. '-', '--', ':', '-.'
                linewidth=0.5,
                color="gray",
                alpha=0.7,
            )

    plt.tight_layout()
    for path in paths:
        if base_dir:
            plt.savefig(os.path.join(base_dir, path))
        else:
            plt.savefig(path)


problem_styles = {
    "SI_c UpdateSer_c\tSer_c": Style(
        problem_as_latex("SI_c UpdateSer_c\tSer_c", simple=True), BLUE, "s", "--"
    ),
    "SI_c UpdateSer_b\tSer_c": Style(
        problem_as_latex("SI_c UpdateSer_b\tSer_c", simple=True), ORANGE, "^", ":"
    ),
    "Ser_b\tSI_b": Style(problem_as_latex("Ser_b\tSI_b", simple=True), GREEN, "o", "-"),
    "SI_c\tSI_b": Style(problem_as_latex("SI_c\tSI_b", simple=True), PURPLE, "^", "--"),
}

problems_rq1 = [
    "SI_c UpdateSer_c\tSer_c",
    "SI_c UpdateSer_b\tSer_c",
    "Ser_b\tSI_b",
    "SI_c\tSI_b",
]


# Given a cleaned dataframe, draw a matrix of plots for the given `specs`.
# Specs is a list of pairs (satisfied, violated)
def plot_rq1(
    df,
    problems=problems_rq1,
    implementation="default",
    logScaling=False,
    plotHeight=4.0,
    plotWidth=5,
    paths=[],
    base_dir=None,
    unit="auto",
):
    assert unit in ["auto", "s", "ms"]

    fig, ax = plt.subplots(figsize=(plotWidth, plotHeight))

    # We calculate the maximum y-limit for this row
    max_y = df[
        (df["problem"].isin(problems)) & (df["implementation"] == implementation)
    ]["max_time_ms"].max()
    min_y = df[
        (df["problem"].isin(problems)) & (df["implementation"] == implementation)
    ]["min_time_ms"].min()
    print(max_y)
    print(min_y)

    if unit == "auto":
        # Use `y_lim` to infer the correct unit to use
        scale_factor, unit = determine_unit(max_y)
    elif unit == "s":
        scale_factor, unit = 1000, "s"
    else:
        scale_factor, unit = 1, "ms"

    for problem in problems:
        subset = df[
            (df["problem"] == problem)
            & (df["solver"] == "glucose")
            & (df["implementation"] == implementation)
        ]
        ax.errorbar(
            subset["num_txn"],
            subset["avg_time_ms"],
            yerr=[
                subset["avg_time_ms"] - subset["min_time_ms"],
                subset["max_time_ms"] - subset["avg_time_ms"],
            ],
            capsize=3,
            markersize=5,
            label=problem_styles[problem].name,
            color=problem_styles[problem].color,
            marker=problem_styles[problem].marker,
            linestyle=problem_styles[problem].linestyle,
        )

        print(0.95 * min_y)
        if logScaling == True or (logScaling != False and problem in logScaling):
            ax.set_yscale("log")
            ax.set_ylim(bottom=0.95 * min_y, top=1.15 * max_y)
        else:
            ax.set_ylim(bottom=0, top=1.05 * max_y)

    # Use an appropriate unit for they y-axis
    ax.yaxis.set_major_formatter(get_formatter(scale_factor))

    label = f"Time ({unit})"
    ax.set_ylabel(label)

    ax.set_xlabel("Number of Transactions")

    ax.legend(loc="upper left")

    ax.grid(
        True,  # turn grid on
        which="both",  # 'major', 'minor', or 'both'
        axis="both",  # 'x', 'y', or 'both'
        linestyle="--",  # e.g. '-', '--', ':', '-.'
        linewidth=0.5,
        color="gray",
        alpha=0.7,
    )

    plt.tight_layout()
    for path in paths:
        if base_dir:
            plt.savefig(os.path.join(base_dir, path))
        else:
            plt.savefig(path)


problems_new_table1 = [
    "SI_b UpdateSer_b\tSer_b",
    "SI_b UpdateSer_c\tSer_c",
    "CC_b\tPlumeCC_b",
    "RA_b\tRA_c",
]
