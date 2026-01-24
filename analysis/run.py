import pandas as pd
from helper import *

edges = [("RA", "CC"), ("CC", "PC"), ("PC", "SI"), ("SI", "Ser")]


def sat_problems_same_framework():
    df_biswas = clean(
        pd.read_csv(
            "/home/mane/Desktop/vldb-experiments/updated/3_sessions/sat_biswas.csv"
        )
    )
    df_cerone = clean(
        pd.read_csv(
            "/home/mane/Desktop/vldb-experiments/updated/3_sessions/sat_cerone.csv"
        )
    )
    height = 1.2
    width = 2

    edges_cerone = [
        (weaker + "_Cerone", stronger + "_Cerone") for (weaker, stronger) in edges
    ]
    # logScaling = [ (f"{weaker}_Cerone", f"{stronger}_Cerone") for (weaker, stronger) in [("RA", "CC"), ("SI", "Ser")] ]
    plot_specs(
        df_cerone,
        edges_cerone,
        base_dir="/home/mane/isolde/papers/icse/plots",
        paths=["edges_cerone.pgf", "edges_cerone.pdf"],
        logScaling=False,
        implementations=["no_fixed_sessions"],
        display_level_fun=level_name_as_latex,
        plotHeight=height,
        plotWidth=width,
    )

    edges_biswas = [
        (weaker + "_Biswas", stronger + "_Biswas") for (weaker, stronger) in edges
    ]
    plot_specs(
        df_biswas,
        edges_biswas,
        base_dir="/home/mane/isolde/papers/icse/plots",
        paths=["edges_biswas.pgf", "edges_biswas.pdf"],
        logScaling=False,
        implementations=["no_fixed_sessions"],
        display_level_fun=level_name_as_latex,
        plotHeight=height,
        plotWidth=width,
    )


def sat_problems_diff_framework():
    df = clean(
        pd.read_csv(
            "/home/mane/Desktop/vldb-experiments/updated/3_sessions/sat_across_frameworks.csv"
        )
    )
    height = 1.2
    width = 2

    edges_cerone_biswas = [
        (weaker + "_Cerone", stronger + "_Biswas") for (weaker, stronger) in edges
    ]
    plot_specs(
        df,
        edges_cerone_biswas,
        implementations=["no_fixed_sessions"],
        base_dir="/home/mane/isolde/papers/icse/plots",
        paths=["edges_cerone_biswas.pgf"],
        logScaling=False,
        display_level_fun=level_name_as_latex,
        plotHeight=height,
        plotWidth=width,
    )

    edges_biswas_cerone = [
        (weaker + "_Biswas", stronger + "_Cerone") for (weaker, stronger) in edges
    ]
    # logScaling = [ (f"{weaker}_Biswas", f"{stronger}_Cerone") for (weaker, stronger) in [("RA", "CC"), ("SI", "Ser")] ]
    plot_specs(
        df,
        edges_biswas_cerone,
        implementations=["no_fixed_sessions"],
        base_dir="/home/mane/isolde/papers/icse/plots",
        paths=["edges_biswas_cerone.pgf"],
        logScaling=False,
        display_level_fun=level_name_as_latex,
        plotHeight=height,
        plotWidth=width,
    )


def unsat_problems_diff_frameworks():
    df = clean(
        pd.read_csv("/home/mane/Desktop/vldb-experiments/equiv_diff_frameworks.csv")
    )
    height = 1.2
    width = 2

    levels = ["RA", "CC", "PC", "SI", "Ser"]

    equiv_cerone_biswas = [(level + "_Cerone", level + "_Biswas") for level in levels]
    equiv_biswas_cerone = [(level + "_Biswas", level + "_Cerone") for level in levels]

    plot_specs(
        df,
        equiv_cerone_biswas,
        base_dir="/home/mane/isolde/papers/vldb/plots",
        paths=["unsat_cerone_biswas.pgf"],
        logScaling=False,
        display_level_fun=level_name_as_latex,
        plotHeight=height,
        plotWidth=width,
    )

    plot_specs(
        df,
        equiv_biswas_cerone,
        base_dir="/home/mane/isolde/papers/vldb/plots",
        paths=["unsat_biswas_cerone.pgf"],
        logScaling=False,
        display_level_fun=level_name_as_latex,
        plotHeight=height,
        plotWidth=width,
    )


def vldb_plots():
    df = clean(
        pd.read_csv("/home/mane/vldb_measurements/data.csv"),
        check_num_measurements=False,
    )
    base = "/home/mane/isolde_vldb/plots/vldb"
    height = 3
    width = 4

    plot_problems(
        df,
        ["SI_c UpdateSer_c\tSer_c"],
        base_dir=base,
        implementations=["default"],
        paths=["sat_single.pgf"],
        logScaling=False,
        plotHeight=height,
        plotWidth=width,
        unit="s",
    )

    plot_problems(
        df,
        ["SI_c UpdateSer_b\tSer_c"],
        base_dir=base,
        implementations=["default"],
        paths=["sat_multi.pgf"],
        logScaling=False,
        plotHeight=height,
        plotWidth=width,
        unit="s",
    )

    plot_problems(
        df,
        ["Ser_b\tSI_b"],
        base_dir=base,
        implementations=["default"],
        paths=["unsat_single.pgf"],
        logScaling=False,
        plotHeight=height,
        plotWidth=width,
        unit="s",
    )

    plot_problems(
        df,
        ["SI_c\tSI_b"],
        base_dir=base,
        implementations=["default"],
        paths=["unsat_multi.pgf"],
        logScaling=False,
        plotHeight=height,
        plotWidth=width,
        unit="s",
    )


def vldb_plot2():
    df = clean(
        pd.read_csv("/home/mane/vldb_measurements/data.csv"),
        check_num_measurements=False,
        remove_timeouts=True,
    )
    base = "/home/mane/isolde_vldb/plots/vldb"
    height = 3.5
    width = 5

    plot_problems(
        df,
        ["Ser_b\tSI_b"],
        base_dir=base,
        implementations=[
            "default",
            "without fixed order",
            "without smart search",
        ],
        paths=["ablation.pgf"],
        logScaling=True,
        plotHeight=height,
        plotWidth=width,
        unit="s",
        legend=True,
    )


def vldb_plot1():
    df = clean(
        pd.read_csv("/home/mane/vldb_measurements/data.csv"),
        check_num_measurements=False,
    )
    base = "/home/mane/isolde_vldb/plots/vldb"
    height = 3.5
    width = 5
    plot_rq1(
        df,
        logScaling=True,
        plotHeight=height,
        plotWidth=width,
        paths=["rq1.pgf", "rq1.pdf"],
        base_dir=base,
        unit="ms",
    )


if __name__ == "__main__":
    df = clean(
        pd.read_csv("/home/mane/vldb_measurements/revision/test.csv"),
        check_num_measurements=False,
        remove_timeouts=True,
    )

    plot_problems(
        df,
        ["Ser_b\tSI_b"],
        base_dir="/home/mane/isolde_vldb/plots/revision/",
        implementations=[
            "default",
            "without fixed order",
            "without smart search",
            "without incremental search",
            "without incremental search",
        ],
        paths=["test.pdf"],
        logScaling=True,
        plotHeight=3.5,
        plotWidth=5,
        unit="s",
        legend=True,
    )
