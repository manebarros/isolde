import itertools
from typing import List

from domain import Definition, Problem
from plotting import Style
from preprocessing import merge_rows


def clean_rq1(df, setup, txn_max_lim=None):
    df = merge_rows(
        df,
        setup=setup,
        txn_max_lim=txn_max_lim,
        check_num_measurements=False,
        implementations=["default"],
    )
    df["sat"] = df["problem"].apply(lambda p: "SAT" if sat(p) else "UNSAT")
    return df


def clean_rq2(df, setup, txn_max_lim=None):
    df = clean_rq1(df, setup=setup, txn_max_lim=txn_max_lim)
    df["optimizations"] = df["implementation"].apply(optimizations)
    df = df[df["problem"].isin(problems_rq1[2:])]
    return df
