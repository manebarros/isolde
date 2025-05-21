#!/usr/bin/env python
# coding: utf-8

import os
import util
import pandas as pd

DATA_PATH = "/home/mane/mestrado/tese/data/synthesis/"
ALLOYSTAR_DATA     = DATA_PATH + "alloystar_data.csv"
FORMULA_BASED_DATA = DATA_PATH + "data.csv"
BOUNDS_BASED_DATA  = DATA_PATH + "bounds_based_data.csv"
FORMULA_FIXED_DATA  = DATA_PATH + "formula_based_fixed.csv"

SPECS = [
    ("RA", "CC"),
    ("CC", "PC"),
    ("CC", "PSI"),
    ("PSI", "SI"),
    ("PC", "SI"),
    ("SI", "SER"),
]


ENCODINGS = [
    ("optimized", "Optimized"),
    ("not_optimized", "Not optimized"),
]

def spec_names():
    names = {}
    for spec in SPECS:
        names[spec] = display_spec(spec)
    return names

def spec_priority(spec):
    return spec[0]

def build_dataframe_list(files):
    dfs = []
    for file in files:
        dfs.append(pd.read_csv(file))
    df = pd.concat(dfs)
    assert df['obj'].nunique() == 1 and df['val'].nunique() == 1
    return df

def build_dataframe(formula_based_data=FORMULA_FIXED_DATA, 
                    bounds_based_data=BOUNDS_BASED_DATA, 
                    alloystar_data=ALLOYSTAR_DATA):
    df_formula = pd.read_csv(formula_based_data)
    df_bounds = pd.read_csv(bounds_based_data)
    df_alloystar = pd.read_csv(alloystar_data)

    final_df = pd.concat([df_formula, df_bounds, df_alloystar])
    assert final_df['obj'].nunique() == 1 and final_df['val'].nunique() == 1
    return final_df

# Takes a filepath and returns a dataframe with the desired format:
# it merges together runs for the same configuration
def preprocess(df, lim=8):
    df = df.groupby(['encoding', 'solver', 'satisfied', 'violated', 'txn', 'ops', 'obj', 'val']).agg(
        avg_cand=('candidates', 'mean'),
        min_cand=('candidates', 'min'),
        max_cand=('candidates', 'max'),
        avg_time=('time', 'mean'),
        min_time=('time', 'min'),
        max_time=('time', 'max')
    ).reset_index()

    # Create the new column 'spec' with tuples of 'satisfied' and 'violated'
    df['spec'] = df.apply(lambda row: (row['satisfied'], row['violated']), axis=1)

    # Drop the original columns 'satisfied' and 'violated'
    df.drop(['satisfied', 'violated'], axis=1, inplace=True)

    if lim:
        df = df[df['txn'] <= lim]

    return df

def display_spec(spec):
    satisfied, violated = spec
    return fr"$\left\{{{satisfied},\overline{{{violated}}}\right\}}$"

def drawEncodingComparison(data=build_dataframe(), specs=None, excludeEncodings=[], logScaling=True, legendSize=(2, 0.88)):
    df = preprocess(data, lim=10)
    if specs:
        df = df[df['spec'].isin(specs)]
    if excludeEncodings:
        df = df[~df['encoding'].isin(excludeEncodings)]
    label_names = dict(ENCODINGS)
    return util.plotMatrixTimePerTxn(
        df, 'spec', 'solver', 'encoding',
        display_spec, util.rename_solver, label_names, util.get_label_colors(label_names.keys()), 
        logScaling=logScaling, row_ord_func=lambda x: util.priority(x[0]), label_ord_fun=None, legendSize=legendSize
    )

def drawLevelComparison(data, encoding='bounds_based_simple', specs=None):
    df = preprocess(data)
    df = df[df['encoding'] == encoding]
    df = df.drop('encoding', axis=1)
    if specs:
        df = df[df['spec'].isin(specs)]
    label_names = spec_names()
    label_colors = util.get_label_colors(SPECS)
    return util.plotMatrixTimePerTxn(
        df, None, 'solver', 'spec', None, util.rename_solver, label_names, label_colors,
        logScaling=False, row_ord_func=None, label_ord_fun=lambda x: util.priority(x[0]), legendSize=(3, 0.78) 
    )

def drawEncodingComparisonCandidates(data=build_dataframe(), specs=None, excludeEncodings=[], logScaling=False):
    print(specs)
    df = preprocess(data)
    if specs:
        df = df[df['spec'].isin(specs)]
    if excludeEncodings:
        df = df[~df['encoding'].isin(excludeEncodings)]
    label_names = dict(ENCODINGS)
    return util.plotMatrixCandidatesPerTxn(
        df, 'spec', 'solver', 'encoding',
        display_spec, util.rename_solver, label_names, util.get_label_colors(label_names.keys()),
        logScaling=logScaling, row_ord_func=lambda x: util.priority(x[0]), label_ord_fun=None, legendSize=(2, 0.88)
    )

def drawLevelComparisonCandidates(data, encoding='bounds_based_simple', specs=None):
    df = preprocess(data)
    df = df[df['encoding'] == encoding]
    df = df.drop('encoding', axis=1)
    if specs:
        df = df[df['spec'].isin(specs)]
    return util.plotMatrixCandidatesPerTxn(
        df, None, 'solver', 'spec',
        None, util.rename_solver, spec_names(), util.get_label_colors(SPECS),
        logScaling=False, row_ord_func=None, label_ord_fun=lambda x: util.priority(x[0]), legendSize=(3, 0.78)
    )

if __name__ == "__main__":
    dir = "fig/pgf"
    if not os.path.exists(dir):
        os.makedirs(dir)

    df = build_dataframe()
    #drawEncodingComparison(specs=specs, excludeEncodings=excluded).savefig(os.path.join(dir, "encodings_time.pgf"))
    #drawEncodingComparisonCandidates(df, specs=specs, excludeEncodings=excluded, logScaling=True).savefig(os.path.join(dir, "encodings_candidates.pgf"))
    #drawLevelComparison(df).savefig(os.path.join(dir, "levels_time.pgf"))
    #drawLevelComparisonCandidates(df).savefig(os.path.join(dir, "levels_candidates.pgf"))

    # For the presentation
    #excluded = ["bounds_based_simple_without_fixed_ar", "formula_based_fixed", "formula_based_fixed_alt", "bounds_based"]
    #drawEncodingComparison(specs=[("RA", "CC")], excludeEncodings=excluded, legendSize=(2, 0.8)).savefig(os.path.join("/home/mane/mestrado/presentation/latex/plots", "cegis.pdf"))

    # For the paper
    #drawEncodingComparison(specs=specs, excludeEncodings=excluded, legendSize=None).savefig(os.path.join("/home/mane/isolde/paper/plots/synthesis", "encodings_time.pgf"))
    #drawEncodingComparison(specs=specs, excludeEncodings=excluded, legendSize=None).savefig(os.path.join("/home/mane/isolde/paper/plots/synthesis", "linhas.pdf"))

    # For refactor
    new_measurements = "/home/mane/cav_evaluation/rebuttal_keys.csv"
    plot = drawEncodingComparison(data=build_dataframe_list([new_measurements]),  logScaling=True, specs=None, excludeEncodings=[], legendSize=(2, 0.92))
    dir = "/home/mane/cav_evaluation/plots/"
    name = "synthesis_times_different_key_sets"
    plot.savefig(os.path.join(dir, name + ".pgf"))
    plot.savefig(os.path.join(dir, name + ".pdf"))
