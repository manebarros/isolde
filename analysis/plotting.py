import os
from dataclasses import dataclass

import matplotlib.pyplot as plt
import numpy as np


@dataclass
class Style:
    name: str
    color: str
    marker: str
    linestyle: str


def determine_unit(max_val):
    if max_val < 400:
        return 1, "ms"
    else:
        return 1000, "s"


def get_formatter(scale_factor):
    return lambda v, _: f"{int(v / scale_factor)}"


def plot(
    df,
    col_field,
    row_field,
    line_field,
    styles=None,
    logScaling=False,
    plotHeight=4,
    plotWidth=5,
    paths=[],
    base_dir=None,
    display_level_fun=None,
    legend=False,
    unit="auto",
):
    assert unit in ["auto", "s", "ms"]

    col_vals = df[col_field].unique()
    row_vals = df[row_field].unique()
    line_vals = df[line_field].unique()

    # Create subplot grid
    n_rows = len(row_vals)
    n_cols = len(col_vals)

    _, axes = plt.subplots(
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
    for i, row_val in enumerate(row_vals):

        # We calculate the maximum y-limit for this row
        max_y = df[(df[row_field] == row_val)]["max_time_ms"].max()

        min_y = df[(df[row_field] == row_val)]["min_time_ms"].min()

        match unit:
            case "auto":
                # Use `y_lim` to infer the correct unit to use
                scale_factor, unit = determine_unit(max_y)
            case "s":
                scale_factor, unit = 1000, "s"
            case _:
                scale_factor, unit = 1, "ms"

        for j, col_val in enumerate(col_vals):
            ax = axes[i, j]

            for line_val in line_vals:
                subset = df[
                    (df[row_field] == row_val)
                    & (df[col_field] == col_val)
                    & (df[line_field] == line_val)
                ]

                if styles:
                    bar_style = {
                        "label": styles[line_val].name if len(line_vals) > 1 else None,
                        "color": styles[line_val].color,
                        "marker": styles[line_val].marker,
                        "linestyle": styles[line_val].linestyle,
                    }
                else:
                    bar_style = {}

                ax.errorbar(
                    subset["num_txn"],
                    subset["avg_time_ms"],
                    yerr=[
                        subset["avg_time_ms"] - subset["min_time_ms"],
                        subset["max_time_ms"] - subset["avg_time_ms"],
                    ],
                    capsize=3,
                    markersize=5,
                    **bar_style,
                )

            if logScaling == True or (logScaling != False and row_val in logScaling):
                ax.set_yscale("log")
                ax.set_ylim(bottom=0.95 * min_y, top=1.15 * max_y)
            else:
                ax.set_ylim(bottom=0, top=1.05 * max_y)

            # Use an appropriate unit for they y-axis
            ax.yaxis.set_major_formatter(get_formatter(scale_factor))

            # Only the top row gets titles
            if i == 0 and n_cols > 1:
                ax.set_title(col_val)

            # Only the left-most column gets y-axis labels
            if j == 0:
                label = f"Time ({unit})"
                if display_level_fun != None:
                    label = display_level_fun(row_val) + "\n\n" + label
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
