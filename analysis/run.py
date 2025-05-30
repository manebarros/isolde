import pandas as pd
from helper import *

edges = [("RA", "CC"), ("CC", "PC"), ("PC", "SI"), ("SI", "Ser")]

def sat_problems_same_framework():
    df = clean(pd.read_csv("/home/mane/Desktop/vldb-experiments/edges_same_framework.csv"))
    height = 1.2
    width = 2

    edges_cerone = [ (weaker + "_Cerone", stronger + "_Cerone") for (weaker, stronger) in edges ]
    logScaling = [ (f"{weaker}_Cerone", f"{stronger}_Cerone") for (weaker, stronger) in [("RA", "CC"), ("SI", "Ser")] ]
    plot_specs(df, edges_cerone, 
               paths=["/home/mane/isolde/papers/vldb/plots/edges_cerone.pgf", "/home/mane/proj/minsolde/analysis/plots/edges_cerone.pdf"], 
               logScaling=logScaling, 
               display_level_fun=level_name_as_latex,
               plotHeight=height, plotWidth=width)

    edges_biswas = [ (weaker + "_Biswas", stronger + "_Biswas") for (weaker, stronger) in edges ]
    plot_specs(df, edges_biswas, 
               paths=["/home/mane/isolde/papers/vldb/plots/edges_biswas.pgf", "/home/mane/proj/minsolde/analysis/plots/edges_biswas.pdf"], 
               logScaling=False, 
               display_level_fun=level_name_as_latex, 
               plotHeight=height, plotWidth=width)

def sat_problems_diff_framework():
    df = clean(pd.read_csv("/home/mane/Desktop/vldb-experiments/edges_diff_frameworks.csv"))
    height = 1.2
    width = 2

    edges_cerone_biswas = [ (weaker + "_Cerone", stronger + "_Biswas") for (weaker, stronger) in edges ]
    plot_specs(df, edges_cerone_biswas, 
               base_dir="/home/mane/isolde/papers/vldb/plots", 
               paths=["edges_cerone_biswas.pgf"], 
               logScaling=False, 
               display_level_fun=level_name_as_latex,
               plotHeight=height, plotWidth=width)

    edges_biswas_cerone = [ (weaker + "_Biswas", stronger + "_Cerone") for (weaker, stronger) in edges ]
    logScaling = [ (f"{weaker}_Biswas", f"{stronger}_Cerone") for (weaker, stronger) in [("RA", "CC"), ("SI", "Ser")] ]
    plot_specs(df, edges_biswas_cerone, 
               base_dir="/home/mane/isolde/papers/vldb/plots", 
               paths=["edges_biswas_cerone.pgf"], 
               logScaling=logScaling,
               display_level_fun=level_name_as_latex,
               plotHeight=height, plotWidth=width)

def unsat_problems_diff_frameworks():
    df = clean(pd.read_csv("/home/mane/Desktop/vldb-experiments/equiv_diff_frameworks.csv"))
    height = 1.2
    width = 2
    
    levels = ["RA", "CC", "PC", "SI", "Ser"]

    equiv_cerone_biswas = [ (level + "_Cerone", level + "_Biswas") for level in levels ]
    equiv_biswas_cerone = [ (level + "_Biswas", level + "_Cerone") for level in levels ]

    plot_specs(df, equiv_cerone_biswas,
               base_dir="/home/mane/isolde/papers/vldb/plots", 
               paths=["unsat_cerone_biswas.pgf"], 
               logScaling=False, 
               display_level_fun=level_name_as_latex,
               plotHeight=height, plotWidth=width)

    plot_specs(df, equiv_biswas_cerone, 
               base_dir="/home/mane/isolde/papers/vldb/plots", 
               paths=["unsat_biswas_cerone.pgf"], 
               logScaling=False,
               display_level_fun=level_name_as_latex,
               plotHeight=height, plotWidth=width)

if __name__ == '__main__':
    sat_problems_same_framework()
    #sat_problems_diff_framework()
    #unsat_problems_diff_frameworks()
