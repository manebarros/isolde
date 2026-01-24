from dataclasses import dataclass
from enum import StrEnum
from typing import List


class Framework(StrEnum):
    B = "b"
    C = "c"

    def as_latex(self) -> str:
        return rf"\mathcal{{{self.upper()}}}"


@dataclass
class Definition:
    name: str
    framework: Framework

    @classmethod
    def from_str(cls, name: str):
        (level, fw) = name.split("_")
        return cls(level, Framework(fw))

    def as_latex(self, use_sc=False, with_fw=True) -> str:
        match self.name:
            case "UpdateSer":
                level_name = "US"
            case "PlumeRA":
                level_name = "TapRA"
            case "PlumeCC":
                level_name = "TapCC"
            case _:
                level_name = self.name

        if use_sc:
            level_name = rf"\textsc{{{level_name}}}"
        return (
            rf"{level_name}_{{{self.framework.as_latex()}}}" if with_fw else level_name
        )


@dataclass
class Problem:
    pos: List[Definition]
    neg: Definition

    @classmethod
    def from_str(cls, name: str):
        (pos, neg) = name.split("\t")
        pos_lst = pos.split(" ")
        return cls([Definition.from_str(d) for d in pos_lst], Definition.from_str(neg))

    def problem_as_latex(self, use_dollar_sign=True, use_sc=False) -> str:
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
