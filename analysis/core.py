import pandas as pd

setup = ['implementation', 'solver', 'satisfied', 'violated', 'num_txn', 'num_keys', 'num_values', 'num_sessions']

# Tests if:
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

# Does basic cleanup of source dataframe: groups together measurements with the same setup, keeping the number of candidates of each group
def prepare(df):
    assert validate(df)[0]
    agg_times = df.groupby(setup)['time_ms'].agg(
        avg_time_ms='mean',
        min_time_ms='min',
        max_time_ms='max')

    agg_candidates = df.groupby(setup)['candidates'].first()
    return pd.concat([agg_times, agg_candidates], axis=1).reset_index()

