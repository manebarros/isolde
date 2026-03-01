def full_results_table(df):
    ptypes = df['problem_type'].unique()
    scopes = df['num_txn'].unique()
    implementations = df['implementation'].unique()
    metrics = ['avg_time_ms', 'candidates']
    s = r"""\begin{table}[h]
\centering
\begin{tabular}{"""
    s += "ccc"
    for _ in implementations:
        s += "c"
    s += "}\n"


    for ptype_i, ptype in enumerate(ptypes):
        problems = df[df['problem_type'] == ptype]['problem'].unique()
        s += "\\multirow{" + f"{len(problems) * len(scopes) * len(metrics)}" + "}{*}{"+ str(ptype) +"}"
        for prob_i, prob in enumerate(problems):
            s += "& \\multirow{" + f"{len(scopes) * len(metrics)}" + "}{*}{"+ prob.as_latex() +"}"
            for scope_i, scope in enumerate(scopes):
                if scope_i > 0:
                    s += "&"
                s += "& \\multirow{" + f"{len(metrics)}" + "}{*}{"+ str(scope) +"}"
                for i, metric in enumerate(metrics):
                    if i > 0:
                        s += "&&"
                    for implementation in implementations:
                        subset = df[
                            (df['problem_type'] == ptype)
                            & (df['problem'] == prob)
                            & (df['num_txn'] == scope)
                            & (df['implementation'] == implementation)
                        ]
                        assert len(subset) == 1, f"{ptype} {prob} {len(subset)} num_txn: {scope} {implementation}"
                        outcome = subset['outcome'].iloc[0]
                        match outcome:
                            case 'CRASH':
                                string = "\\multirow{2}{*}{C}" if i == 0 else ""
                            case 'TIMEOUT':
                                string = "\\multirow{2}{*}{TO}" if i == 0 else ""
                            case _:
                                string = f"{subset[metric].iloc[0]}"
                        s += f"& {string}"
                    s += "\\\\ \n\\midrule\n" 
    s += r"""
\end{tabular}
\end{table}
"""
    return s

if __name__ == "__main__":
    import pandas as pd
    import preprocessing as pre
    from vldb import fill_with_timeouts
    df = pd.read_csv("/home/mane/code/isolde/isolde-experiments/data/d8dfd9f4814950.csv")
    df = pre.preprocess(df)
    df = fill_with_timeouts(df)
    print(full_results_table(df))
