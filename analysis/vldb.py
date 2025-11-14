from typing import List

from helper import *
from helper import framework, parse

levels = ["RA", "CC", "PC", "SI", "Ser"]

edges = [("RA", "CC"), ("CC", "PC"), ("PC", "SI"), ("SI", "Ser")]


def satSameFramework():
    problems = []
    for pos, neg in edges:
        problems.append(f"{pos}_b\t{neg}_b")
        problems.append(f"{pos}_c\t{neg}_c")
    problems.append("SI_b UpdateSer_b\tSer_b")
    problems.append("SI_c UpdateSer_c\tSer_c")
    problems.append("PlumeRA_b\tRA_b")
    return problems


def satDiffFramework():
    problems = []
    for pos, neg in edges:
        problems.append(f"{pos}_b\t{neg}_c")
        problems.append(f"{pos}_c\t{neg}_b")

    for si in ["SI_c", "SI_b"]:
        for updateSer in ["UpdateSer_c", "UpdateSer_b"]:
            for ser in ["Ser_c", "Ser_b"]:
                fws = {framework(spec) for spec in [si, updateSer, ser]}
                if len(fws) > 1:
                    problems.append(f"{si} {updateSer}\t{ser}")
    return problems


def unsatDiffFramework():
    problems = []
    for level in levels:
        problems.append(f"{level}_b\t{level}_c")
        problems.append(f"{level}_c\t{level}_b")
    return problems


def unsatSameFramework():
    problems = []
    for pos, neg in edges:
        problems.append(f"{neg}_b\t{pos}_b")
        problems.append(f"{neg}_c\t{pos}_c")
    problems.append("RA_b\tPlumeRA_b")
    problems.append("CC_b\tPlumeCC_b")
    problems.append("PlumeCC_b\tCC_b")
    return problems


print(len(satSameFramework()))
print(len(satDiffFramework()))
print(len(unsatSameFramework()))
print(len(unsatDiffFramework()))

import itertools

all_problems = [
    satSameFramework(),
    satDiffFramework(),
    unsatSameFramework(),
    unsatDiffFramework(),
]

all_problems_flatten = [item for sublist in all_problems for item in sublist]

all_disjoint = all(
    set(x).isdisjoint(y) for x, y in itertools.combinations(all_problems, 2)
)

print(all_disjoint)


def sat(problem):
    return problem in satSameFramework() or problem in satDiffFramework()


def unsat(problem):
    return not sat(problem)


def clean_rq1(df, setup=setup, txn_max_lim=None):
    validation_result = validate(df, setup, check_num_measurements=False)
    assert validation_result[0], validation_result[1]
    if txn_max_lim:
        df = df[df["num_txn"] <= txn_max_lim]
    df = df[df["implementation"] == "default"]
    df = (
        df.groupby(setup)
        .agg(
            candidates=("candidates", "first"),
            avg_time_ms=("time_ms", "mean"),
            min_time_ms=("time_ms", "min"),
            max_time_ms=("time_ms", "max"),
        )
        .reset_index()
    )
    df["avg_time_ms"] = df["avg_time_ms"].round().astype(int)
    df["sat"] = df["problem"].apply(lambda p: "SAT" if sat(p) else "UNSAT")
    df["frameworks"] = df["problem"].apply(lambda p: showframeworks(frameworks(p)))
    return df


def optimizations(implementation: str):
    match implementation:
        case "default":
            return r"\(\{ S, \, T\}\)"
        case "without fixed order":
            return r"\(\{ S \}\)"
        case "without smart search":
            return r"\(\{ T \}\)"
        case "no optimizations":
            return r"\(\emptyset\)"


def clean_rq2(df, setup=setup, txn_max_lim=None):
    validation_result = validate(df, setup, check_num_measurements=False)
    assert validation_result[0], validation_result[1]
    if txn_max_lim:
        df = df[df["num_txn"] <= txn_max_lim]
    df = (
        df.groupby(setup)
        .agg(
            candidates=("candidates", "first"),
            avg_time_ms=("time_ms", "mean"),
            min_time_ms=("time_ms", "min"),
            max_time_ms=("time_ms", "max"),
        )
        .reset_index()
    )
    df["avg_time_ms"] = df["avg_time_ms"].round().astype(int)
    df["sat"] = df["problem"].apply(lambda p: "SAT" if sat(p) else "UNSAT")
    df["frameworks"] = df["problem"].apply(frameworks)
    df["frameworks_str"] = df["frameworks"].apply(showframeworks)
    df["optimizations"] = df["implementation"].apply(optimizations)
    # filter only the problems that are unsat and use more than one framework
    # df = df[df["problem"].apply(unsat)]
    # df = df[df["frameworks"].apply(lambda fws: len(fws) > 1)]
    df = df[df["problem"].isin(problems_rq1[2:])]
    return df
