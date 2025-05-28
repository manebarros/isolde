import matplotlib.pyplot as plt
import numpy as np

solver_display_names = { 
    'minisat' : 'MiniSAT', 
    'glucose' : 'Glucose', 
    'sat4j' : 'Sat4j' 
}

# These columns identify a particular configuration for an Isolde run
setup = ['implementation', 'solver', 'satisfied', 'violated', 'num_txn', 'num_keys', 'num_values', 'num_sessions']

# Validates a df. Tests if:
# - all different setups (i.e., implementation + solver + input problem + scope) have the same number of measurements
# - each setup always results in the same number of candidates 
def validate(df):
    counts = df.groupby(setup).size()
    if counts.nunique() != 1:
        return (False, "Not all setups have the same number of measurements")
    grouped = df.groupby(setup)['candidates'].nunique()
    if not (grouped == 1).all():
        return (False, "Not all groups have a consistent 'candidates' value")
    return (True, None)

# cleans a df:
# groups together rows corresponding to the same setup, creating three new columns "avg_time_ms", "max_time_ms", and "min_time_ms"
# we keep the number of candidates of each group 
def clean(df):
    validation_result = validate(df)
    assert validation_result[0], validation_result[1]
    return df.groupby(setup).agg(
        candidates = ('candidates', 'first'),
        avg_time_ms = ('time_ms', 'mean'),
        min_time_ms = ('time_ms', 'min'),
        max_time_ms = ('time_ms', 'max')).reset_index()

def level_name_as_latex(level_name):
    (level, fw) = level_name.split('_')
    return r"\text{" + level + r"}_{\text{" + fw + "}}"

# given a cleaned df, draw a matrix of plots for the given `specs`
# specs is a list of pairs (satisfied, violated)
def plot_specs(df, specs, path=None, logScaling=False):
    col_keys = df['solver'].unique()

    # Create subplot grid
    n_rows = len(specs)
    n_cols = len(col_keys)
    
    fig, axes = plt.subplots(n_rows, n_cols, figsize=(5 * n_cols, 4 * n_rows), sharex=True, sharey=False)
    
    # If only one row or column, make axes 2D
    if n_rows == 1:
        axes = axes[np.newaxis, :]
    if n_cols == 1:
        axes = axes[:, np.newaxis]
    
    # Plot each subplot
    for i, (satisfied, violated) in enumerate(specs):
    
        # We calculate the maximum y-limit for this row
        y_lim = 1.05 * df[
            (df['satisfied'] == satisfied) &
            (df['violated'] == violated)
        ]['max_time_ms'].max()

        min_y = df[
            (df['satisfied'] == satisfied) &
            (df['violated'] == violated)
        ]['min_time_ms'].min()
        
        for j, solver in enumerate(col_keys):
            ax = axes[i, j]
    
            subset = df[
                (df['satisfied'] == satisfied) &
                (df['violated'] == violated) &
                (df['solver'] == solver)
            ]
            ax.errorbar(
                subset['num_txn'],
                subset['avg_time_ms'],
                yerr=[
                    subset['avg_time_ms'] - subset['min_time_ms'],
                    subset['max_time_ms'] - subset['avg_time_ms']
                ],
                fmt='o-', capsize=3
            )
    

            if logScaling:
                ax.set_yscale("log")
                ax.set_ylim(bottom=0.95*min_y, top=y_lim)
            else:
                ax.set_ylim(bottom=0, top=y_lim)

            
            # Only the top row gets titles
            if i == 0:
                ax.set_title(solver_display_names[solver])
    
            # Only the left-most column gets y-axis labels
            if j == 0:
                ax.set_ylabel(r"$\left\{" + level_name_as_latex(satisfied) + r", \, \overline{" + level_name_as_latex(violated) + r"}\right\}$" + "\n\nAverage time (ms)")
            else:
                ax.set_ylabel("")
                ax.set_yticklabels([])
                
    
            # Only the bottom-left gets x-axis label
            if i == n_rows - 1 and j == 0:
                ax.set_xlabel("Number of Transactions")
            else:
                ax.set_xlabel("")          
    
    plt.tight_layout()
    if path:
        plt.savefig(path)
