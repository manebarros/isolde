BLUE = "#1f77b4"
ORANGE = "#ff7f0e"
GREEN = "#2ca02c"
PURPLE = "#800080"

solver_display_names = {"minisat": "MiniSat", "glucose": "Glucose", "sat4j": "Sat4j"}

implementation_styles = {
    "default": Style("Optimized", "#E69F00", "D", "-."),  # orange-yellow
    "no optimizations": Style("No optimization", "#56B4E9", "v", "--"),  # sky blue
    "without fixed order": Style("No fixed txn order", "#009E73", "P", "-"),
    "without smart search": Style("No smart search", "#CC79A7", "X", ":"),
}

# These columns identify a particular configuration for an Isolde run
setup = [
    "implementation",
    "solver",
    "problem",
    "num_txn",
    "num_keys",
    "num_values",
    "num_sessions",
]

problems_new_table1 = [
    "SI_b UpdateSer_b\tSer_b",
    "SI_b UpdateSer_c\tSer_c",
    "CC_b\tPlumeCC_b",
    "RA_b\tRA_c",
]

problem_styles = {
    "SI_c UpdateSer_c\tSer_c": Style(
        problem_as_latex("SI_c UpdateSer_c\tSer_c", simple=True), BLUE, "s", "--"
    ),
    "SI_c UpdateSer_b\tSer_c": Style(
        problem_as_latex("SI_c UpdateSer_b\tSer_c", simple=True), ORANGE, "^", ":"
    ),
    "Ser_b\tSI_b": Style(problem_as_latex("Ser_b\tSI_b", simple=True), GREEN, "o", "-"),
    "SI_c\tSI_b": Style(problem_as_latex("SI_c\tSI_b", simple=True), PURPLE, "^", "--"),
}

problems_rq1 = [
    "SI_c UpdateSer_c\tSer_c",
    "SI_c UpdateSer_b\tSer_c",
    "Ser_b\tSI_b",
    "SI_c\tSI_b",
]
