import matplotlib.pyplot as plt
import pandas as pd

time_color = '#9467bd'
cand_color = '#2ca02c'

# Load your data
df = pd.read_csv("/home/mane/Desktop/vldb-experiments/updated/3_sessions/sat_across_frameworks.csv")

# Filter rows where encoding is "optimized", satisfied is "RA", violated is "CC", and txn <= 10
df_filtered = df[(df['implementation'] == 'no_fixed_sessions') & 
                 (df['satisfied'] == 'CC_Biswas') & 
                 (df['violated'] == 'PC_Cerone') & 
                 (df['solver'] == 'minisat') & 
                 (df['num_txn'] <= 10)]

# Group by 'txn' and compute the mean, min, and max for 'time' and 'candidates'
df_grouped = df_filtered.groupby('num_txn').agg({
    'time_ms': ['mean', 'min', 'max'],
    'candidates': ['mean', 'min', 'max']
}).reset_index()

# Extract relevant columns
txn = df_grouped['num_txn']
time_mean = df_grouped[('time_ms', 'mean')]
time_min = df_grouped[('time_ms', 'min')]
time_max = df_grouped[('time_ms', 'max')]
candidates_mean = df_grouped[('candidates', 'mean')]
candidates_min = df_grouped[('candidates', 'min')]
candidates_max = df_grouped[('candidates', 'max')]

# Calculate the error bars for time and candidates
time_error_low = time_mean - time_min
time_error_high = time_max - time_mean
candidates_error_low = candidates_mean - candidates_min
candidates_error_high = candidates_max - candidates_mean

# Create the figure and first axis
fig, ax1 = plt.subplots(figsize=(5, 3))

# Plot 'time' on the left y-axis with error bars
ax1.errorbar(txn, time_mean, yerr=[time_error_low, time_error_high], fmt='-^', color=time_color, label='Time')
ax1.set_xlabel('Number of transactions')
ax1.set_ylabel('Time (s)')
ax1.tick_params(axis='y')

# Create a second y-axis sharing the same x-axis
ax2 = ax1.twinx()
# Plot 'candidates' on the right y-axis with error bars
ax2.errorbar(txn, candidates_mean, yerr=[candidates_error_low, candidates_error_high], fmt=':.', color=cand_color, label='Candidates')
ax2.set_ylabel('Candidates')
ax2.tick_params(axis='y')

# Add a legend
fig.legend(loc='upper left', bbox_to_anchor=(0.31, 0.95))
plt.tight_layout()

plt.savefig('/home/mane/isolde/papers/icse/plots/time_candidate.pdf', format='pdf')
plt.savefig('/home/mane/isolde/papers/icse/plots/time_candidate.pgf', format='pgf')

