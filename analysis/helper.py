import os

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
    "default": Style("Optimized", BLUE, "s", "--"),
    "without incremental": Style("No incremental solving", ORANGE, "^", ":"),
    "without fixed order": Style("No fixed txn order", GREEN, "o", "-"),
    "without smart search": Style("No smart search", PURPLE, "^", "--"),
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


# Validates a df. Tests if:
# - all different setups (i.e., implementation + solver + input problem + scope) have the same number of measurements
# - each setup always results in the same number of candidates
def validate(df, setup):
    counts = df.groupby(setup).size()
    if counts.nunique() != 1:
        return (False, "Not all setups have the same number of measurements")
    grouped = df.groupby(setup)["candidates"].nunique()
    if not (grouped == 1).all():
        return (False, "Not all groups have a consistent 'candidates' value")
    return (True, None)


# Cleans a dataframe by grouping together rows corresponding to the same setup,
# creating three new columns "avg_time_ms", "max_time_ms", and "min_time_ms".
# We keep the number of candidates of each group.
def clean(df, setup=setup, txn_max_lim=None):
    validation_result = validate(df, setup)
    assert validation_result[0], validation_result[1]
    if txn_max_lim:
        df = df[df["num_txn"] <= txn_max_lim]
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


def level_name_as_latex(level_name):
    (level, fw) = level_name.split("_")
    fw = r"\mathcal{B}" if fw == "Biswas" else r"\mathcal{C}"
    return level + r"_{" + fw + r"}"


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
):
    # The number of solvers
    col_keys = df["solver"].unique()

    if implementations == None:
        implementations = df["implementation"].unique()

    # Create subplot grid
    n_rows = len(problems)
    n_cols = len(col_keys)

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

        # Use `y_lim` to infer the correct unit to use
        scale_factor, unit = determine_unit(max_y)

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
