import itertools
from typing import Dict, List, Tuple

from domain import Definition, Framework, Problem
from plotting import Style

BLUE = "#1f77b4"
ORANGE = "#ff7f0e"
GREEN = "#2ca02c"
PURPLE = "#800080"

levels = ["RA", "CC", "PC", "SI", "Ser"]

edges = [("RA", "CC"), ("CC", "PC"), ("PC", "SI"), ("SI", "Ser")]


def satSameFramework() -> List[Problem]:
    problems = []
    for pos, neg in edges:
        problems.append(f"{pos}_ax_b\t{neg}_ax_b")
        problems.append(f"{pos}_ax_c\t{neg}_ax_c")
    problems.append("SI_ax_b UpdateSer__b\tSer_ax_b")
    problems.append("SI_ax_c UpdateSer__c\tSer_ax_c")
    problems.append("RA_tap_b\tRA_ax_b")
    return [Problem.from_str(s) for s in problems]


def satDiffFramework() -> List[Problem]:
    problems = []
    for pos, neg in edges:
        problems.append(f"{pos}_ax_b\t{neg}_ax_c")
        problems.append(f"{pos}_ax_c\t{neg}_ax_b")

    for si_fw in Framework:
        for updateSer_fw in Framework:
            for ser_fw in Framework:
                if si_fw != updateSer_fw or updateSer_fw != ser_fw:
                    problems.append(
                        f"SI_ax_{si_fw} UpdateSer__{updateSer_fw}\tSer_ax_{ser_fw}"
                    )
    return [Problem.from_str(s) for s in problems]


def unsatDiffFramework() -> List[Problem]:
    problems = []
    for level in levels:
        problems.append(f"{level}_ax_b\t{level}_ax_c")
        problems.append(f"{level}_ax_c\t{level}_ax_b")
    return [Problem.from_str(s) for s in problems]


def unsatSameFramework() -> List[Problem]:
    problems = []
    for pos, neg in edges:
        problems.append(f"{neg}_ax_b\t{pos}_ax_b")
        problems.append(f"{neg}_ax_c\t{pos}_ax_c")
    problems.append("RA_ax_b\tRA_tap_b")
    problems.append("CC_ax_b\tCC_tap_b")
    problems.append("CC_tap_b\tCC_ax_b")
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

assert all_disjoint, "Problem sets are not disjoint!"


def sat(problem: Problem) -> bool:
    return problem in satSameFramework() or problem in satDiffFramework()


def unsat(problem: Problem) -> bool:
    return not sat(problem)


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
