from dataclasses import dataclass
from enum import StrEnum
from typing import List


class Framework(StrEnum):
    BISWAS = "b"
    CERONE = "c"

    def as_latex(self) -> str:
        return rf"\mathcal{{{self.upper()}}}"


class Solver(StrEnum):
    MINISAT = "minisat"
    GLUCOSE = "glucose"
    SAT4J = "sat4j"

    def display_str(self) -> str:
        match self:
            case Solver.MINISAT:
                return "MiniSat"
            case Solver.GLUCOSE:
                return "Glucose"
            case Solver.SAT4J:
                return "Sat4j"


@dataclass(frozen=True, order=True)
class Definition:
    name: str
    framework: Framework

    @classmethod
    def from_str(cls, name: str):
        (level, style, fw) = name.split("_")
        if style == "":
            assert level == "UpdateSer"
            return cls("US", Framework(fw))
        if style == "tap":
            return cls(f"Tap{level}", Framework(fw))
        return cls(level, Framework(fw))

    def as_latex(self, use_sc=False, with_fw=True) -> str:
        level_name = rf"\textsc{{{self.name}}}" if use_sc else self.name
        return (
            rf"{level_name}_{{{self.framework.as_latex()}}}" if with_fw else level_name
        )

    def __str__(self) -> str:
        return f"{self.name}_{self.framework}"


@dataclass(frozen=True, order=True)
class Problem:
    pos: tuple[Definition, ...]
    neg: Definition

    @classmethod
    def from_str(cls, name: str):
        (pos, neg) = name.split("\t")
        pos_lst = pos.split(" ")
        return cls(
            tuple([Definition.from_str(d) for d in pos_lst]), Definition.from_str(neg)
        )

    def __str__(self) -> str:
        return " ".join([l.__str__() for l in self.pos]) + "|" + self.neg.__str__()

    def as_latex(self, use_dollar_sign=True, use_sc=False) -> str:
        pos_str = ", ".join([l.as_latex(use_sc=use_sc) for l in self.pos])
        neg_str = self.neg.as_latex(use_sc=use_sc)
        if use_dollar_sign:
            return rf"$\{{{pos_str}, \, \overline{{{neg_str}}}\}}$"
        else:
            return rf"\(\{{{pos_str}, \, \overline{{{neg_str}}}\}}\)"

    def frameworks(self) -> List[Framework]:
        frameworks = {d.framework for d in self.pos}
        frameworks.add(self.neg.framework)
        frameworks = list(frameworks)
        frameworks.sort()
        return frameworks
