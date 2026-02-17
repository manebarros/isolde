import matplotlib.pyplot as plt
import pandas as pd
import preprocessing as pre
from domain import Problem

time_color = "#9467bd"
cand_color = "#2ca02c"

# Load your data
df = pd.read_csv("../isolde-experiments/data/80b8403f476d5b.csv")

df = pre.preprocess(
    df,
    txn_num=(3, 10),
    problems=[Problem.from_str("SI_ax_c\tSI_ax_b")],
    implementations=["all"],
)

# Extract relevant columns
txn = df["num_txn"]
time_mean = df["avg_time_ms"]
time_min = df["min_time_ms"]
time_max = df["max_time_ms"]
clauses = df["final_clauses"]

# Calculate the error bars for time and candidates
time_error_low = time_mean - time_min
time_error_high = time_max - time_mean

# Create the figure and first axis
fig, ax1 = plt.subplots(figsize=(5, 3))

# Plot 'time' on the left y-axis with error bars
ax1.errorbar(
    txn,
    time_mean,
    yerr=[time_error_low, time_error_high],
    fmt="-^",
    color=time_color,
    label="Time",
)
ax1.set_xlabel("Number of transactions")
ax1.set_ylabel("Time (ms)")
ax1.tick_params(axis="y")

# Create a second y-axis sharing the same x-axis
ax2 = ax1.twinx()
# Plot 'candidates' on the right y-axis with error bars
ax2.errorbar(txn, clauses, color=cand_color, fmt=":.", label="Clauses")
ax2.set_ylabel("clauses")
ax2.tick_params(axis="y")

ax1.grid(
    True,  # turn grid on
    which="both",  # 'major', 'minor', or 'both'
    axis="both",  # 'x', 'y', or 'both'
    linestyle="--",  # e.g. '-', '--', ':', '-.'
    linewidth=0.5,
    color="gray",
    alpha=0.7,
)
# Add a legend
fig.legend(loc="upper left", bbox_to_anchor=(0.31, 0.95))
plt.tight_layout()

plt.savefig("plots/time_clause.pdf", format="pdf")
