import itertools
from typing import List

from domain import Definition, Problem
from plotting import Style
from preprocessing import clean

BLUE = "#1f77b4"
ORANGE = "#ff7f0e"
GREEN = "#2ca02c"
PURPLE = "#800080"

implementation_styles = {
    "default": Style("Optimized", "#E69F00", "D", "-."),  # orange-yellow
    "no optimizations": Style("No optimization", "#56B4E9", "v", "--"),  # sky blue
    "without fixed order": Style("No fixed txn order", "#009E73", "P", "-"),
    "without smart search": Style("No smart search", "#CC79A7", "X", ":"),
}


problems_rq1 = [
    Problem.from_str("SI_c UpdateSer_c\tSer_c"),
    Problem.from_str("SI_c UpdateSer_b\tSer_c"),
    Problem.from_str("Ser_b\tSI_b"),
    Problem.from_str("SI_c\tSI_b"),
]

problems_rq1_colors = [
    (BLUE, "s", "--"),
    (ORANGE, "^", ":"),
    (GREEN, "o", "-"),
    (PURPLE, "^", "--"),
]

problems_rq1_styles = {
    p: Style(p.as_latex(), *colors)
    for p, colors in zip(problems_rq1, problems_rq1_colors)
}

levels = ["RA", "CC", "PC", "SI", "Ser"]

edges = [("RA", "CC"), ("CC", "PC"), ("PC", "SI"), ("SI", "Ser")]


def satSameFramework() -> List[Problem]:
    problems = []
    for pos, neg in edges:
        problems.append(f"{pos}_b\t{neg}_b")
        problems.append(f"{pos}_c\t{neg}_c")
    problems.append("SI_b UpdateSer_b\tSer_b")
    problems.append("SI_c UpdateSer_c\tSer_c")
    problems.append("PlumeRA_b\tRA_b")
    return [Problem.from_str(s) for s in problems]


def satDiffFramework() -> List[Problem]:
    problems = []
    for pos, neg in edges:
        problems.append(f"{pos}_b\t{neg}_c")
        problems.append(f"{pos}_c\t{neg}_b")

    for si in ["SI_c", "SI_b"]:
        for updateSer in ["UpdateSer_c", "UpdateSer_b"]:
            for ser in ["Ser_c", "Ser_b"]:
                fws = {
                    Definition.from_str(spec).framework for spec in [si, updateSer, ser]
                }
                if len(fws) > 1:
                    problems.append(f"{si} {updateSer}\t{ser}")
    return [Problem.from_str(s) for s in problems]


def unsatDiffFramework() -> List[Problem]:
    problems = []
    for level in levels:
        problems.append(f"{level}_b\t{level}_c")
        problems.append(f"{level}_c\t{level}_b")
    return [Problem.from_str(s) for s in problems]


def unsatSameFramework() -> List[Problem]:
    problems = []
    for pos, neg in edges:
        problems.append(f"{neg}_b\t{pos}_b")
        problems.append(f"{neg}_c\t{pos}_c")
    problems.append("RA_b\tPlumeRA_b")
    problems.append("CC_b\tPlumeCC_b")
    problems.append("PlumeCC_b\tCC_b")
    return [Problem.from_str(s) for s in problems]


problem_sets = [
    satSameFramework(),
    satDiffFramework(),
    unsatSameFramework(),
    unsatDiffFramework(),
]

all_problems = [problem for problem_set in problem_sets for problem in problem_set]


all_disjoint = all(
    set(x).isdisjoint(y) for x, y in itertools.combinations(problem_sets, 2)
)

print(f"all problem sets are disjoint: {all_disjoint}")


def sat(problem):
    return problem in satSameFramework() or problem in satDiffFramework()


def unsat(problem):
    return not sat(problem)


def clean_rq1(df, setup, txn_max_lim=None):
    df = clean(
        df,
        setup=setup,
        txn_max_lim=txn_max_lim,
        check_num_measurements=False,
        implementations=["default"],
    )
    df["sat"] = df["problem"].apply(lambda p: "SAT" if sat(p) else "UNSAT")
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


def clean_rq2(df, setup, txn_max_lim=None):
    df = clean_rq1(df, setup=setup, txn_max_lim=txn_max_lim)
    df["optimizations"] = df["implementation"].apply(optimizations)
    df = df[df["problem"].isin(problems_rq1[2:])]
    return df
