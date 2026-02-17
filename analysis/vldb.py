Class = tuple[str, int]

TableData = dict[str, dict[str, dict[int, tuple[tuple[int, int], tuple[int, int]]]]]
Helper = dict[str, dict[str, int]]

timeout = 3600000

def dfToTableData(df, txn_lim=10) -> TableData:
    d = {}
    helper = {}
    for _, row in df.iterrows():
        implementation = row["implementation"]
        num_txn = row["num_txn"]
        expected = row["expected"]
        avg_time_ms = row["avg_time_ms"]
        frameworks = row["frameworks"]
        candidates = row["candidates"]

        if implementation not in d:
            d[implementation] = {}
            helper[implementation] = {}
        problem_class = (expected, frameworks)
        if problem_class not in d[implementation]:
            d[implementation][problem_class] = {}
        if num_txn not in d[implementation][problem_class]:
            d[implementation][problem_class][num_txn] = (
                (avg_time_ms, candidates),
                (avg_time_ms, candidates),
            )

        (min_time, min_cand), (max_time, max_cand) = d[implementation][problem_class][
            num_txn
        ]

        if avg_time_ms < min_time:
            min_time, min_cand = avg_time_ms, candidates
        if avg_time_ms > max_time:
            max_time, max_cand = avg_time_ms, candidates

        d[implementation][problem_class][num_txn] = (
            (min_time, min_cand),
            (max_time, max_cand),
        )

        if avg_time_ms == timeout:
            for i in range(num_txn + 1, txn_lim + 1):
                if i in d[implementation][problem_class]:
                    min_time = d[implementation][problem_class][i][0]
                else:
                    min_time = (timeout + 1000, -1)
                d[implementation][problem_class][i] = (min_time, (avg_time_ms, -1))
    return d


def formatTable2(data: TableData, num_txn=5) -> str:
    s = ""
    for impl, impl_data in data.items():
        s += r"\midrule" + "\n" + r"\multirow{2}{*}{" + impl.replace("_", " ") + r"}"
        for pclass, pclass_data in sorted(impl_data.items()):
            (_, (time, _)) = pclass_data[num_txn]
            s += f" & {time if time < timeout else "\\multirow{2}{*}{TO}"}"
        s += r"\\" + "\n\n"
        for pclass, pclass_data in sorted(impl_data.items()):
            (_, (time, cand)) = pclass_data[num_txn]
            s += f" & {cand if time < timeout else ""}"
        s += r"\\" + "\n\n"
    return s


table2Header = r"""\toprule
                    & SAT single fw & SAT diff fws & UNSAT single fw & UNSAT diff fws \\
"""


def formatTable3(data: TableData) -> str:
    s = ""
    for impl, impl_data in data.items():
        s += r"\midrule" + "\n" + r"\multirow{2}{*}{" + impl.replace("_", " ") + r"}"
        for pclass, pclass_data in sorted(impl_data.items()):
            for num_txn, (_, (time, cand)) in pclass_data.items():
                s += f" & {time if time < timeout else "\\multirow{2}{*}{TO}"}"
        s += r"\\" + "\n"
        for pclass, pclass_data in sorted(impl_data.items()):
            for num_txn, (_, (time, cand)) in pclass_data.items():
                s += f" & {cand if time < timeout else ""}"
        s += r"\\" + "\n\n"
    return s


table3Header = r"""\toprule
                    & \multicolumn{3}{c}{SAT single fw} & \multicolumn{3}{c}{SAT diff fws} & \multicolumn{3}{c}{UNSAT single fw} & \multicolumn{3}{c}{UNSAT diff fws} \\
		\cmidrule(lr){2-13}
                    & 3 & 4 & 5 & 3 & 4 & 5 & 3 & 4 & 5  & 3 & 4 & 5 \\  
"""
