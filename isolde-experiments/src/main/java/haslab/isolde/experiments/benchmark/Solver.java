package haslab.isolde.experiments.benchmark;

import kodkod.engine.satlab.SATFactory;

enum Solver {
  MINISAT("minisat", SATFactory.MiniSat),
  GLUCOSE("glucose", SATFactory.Glucose),
  SAT4J("sat4j", SATFactory.DefaultSAT4J);

  private String id;
  private SATFactory solver;

  private Solver(String id, SATFactory solver) {
    this.id = id;
    this.solver = solver;
  }

  public String getId() {
    return id;
  }

  public SATFactory getSolver() {
    return solver;
  }
}
