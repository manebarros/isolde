# Analysis — building the paper's plots

Builds the figures for the Isolde paper from an `isoldebench` measurement CSV.

## Running

Dependencies are declared in `pyproject.toml` and locked in `uv.lock`; `uv` creates the
environment on first run, so no manual setup is needed.

    uv run plots.py --list                       # what can be built
    uv run plots.py                              # build everything
    uv run plots.py cactus_runtime               # build one figure
    uv run plots.py instances_solved cactus_runtime --dest /tmp/figs

By default it reads `DEFAULT_DATA` (see `plots.py`) and writes to
`analysis/plots/<csv name>/`, which is gitignored. Both the data file and the destination
are printed on every run. Paths are resolved relative to `plots.py`, so the command behaves
the same from any working directory.

Use `--data/-i` to plot a different benchmark run:

    uv run plots.py --data ../experiments/data/3c508f8a48d334.csv

Each run gets its own output directory (named after the CSV), so results from different
data never overwrite each other.

## Figures

Every figure is written as both `.pgf` (what the paper `\input`s) and `.pdf` (for previewing).

| figure | output | paper |
|---|---|---|
| `instances_solved` | `instances_solved.pgf` | `fig:plot1` — instances solved within the timeout |
| `cactus_runtime` | `cactus_runtime.pgf` | `fig:cactus-times` — runtime comparison |
| `cactus_candidates` | `extra/cactus_candidates.pgf` | `fig:cactus-cand` (appendix) |
| `cactus_clauses` | `extra/cactus_clauses.pgf` | `fig:cactus-clauses` (appendix) |
| `scaling_by_implementation` | `extra/scaling_by_implementation.pgf` | `fig:compare-metrics-impl` (appendix) |
| `scaling_by_problem_type` | `extra/scaling_by_problem_type.pgf` | `fig:compare-metrics-ptypes` (appendix) |

The `extra/` split matches the paper's own `plots/` and `plots/extra/` layout, so copying a
built set into the paper repository is a plain directory copy.

## Where the data comes from

    ./benchmark_latest.sh                 # from the repository root
      -> experiments/data/<commit>.csv    # named after the Isolde commit it was run on
      -> uv run plots.py --data ...       # -> analysis/plots/<commit>/

`benchmark_latest.sh` refuses to run on a dirty working tree and names the CSV after
`git rev-parse --short=14 HEAD`, so **the CSV file name identifies the version of Isolde
that produced the measurements**.

## Tracing a plot back to its data

Plots are copied into the paper by hand, so each output records the CSV it was built from.

    $ head -1 cactus_runtime.pgf
    %% data: d8dfd9f4814950.csv

    $ pdfinfo cactus_runtime.pdf | grep Subject
    Subject:        data: d8dfd9f4814950.csv

Because the CSV name is the Isolde commit, that one line identifies both the measurements
and the tool version. No build timestamp is recorded, so rebuilding from the same data
produces byte-identical files and a stray `.pgf` can always be matched to its source.

`matplotlib` is pinned in `pyproject.toml`: its `.pgf` output changes between releases, and
an unpinned version would silently alter the figures. Note that the `.pgf` preamble embeds
the absolute path of the local matplotlib font directory in a comment, so files built on
different machines differ in those (inert) comment lines.

## Other scripts

- `preprocessing.py`, `config.py`, `domain.py`, `plotting.py` — loading, the SAT/UNSAT
  oracle, the problem model, and the generic grid plotter. Imported by `plots.py`.
- `extra_vldb.py` — generates the appendix's full-results LaTeX table. **Currently broken**:
  it asserts exactly one row per (problem type, problem, scope, implementation), but
  `preprocessing.py` deliberately drops `no_smart_search` for multi-framework UNSAT
  problems, so the assertion fails. Pre-existing; the table it emits
  (`appendix-full-table.tex`) is not `\input` by the paper at present.
- `notebooks/` — archived exploratory notebooks. They do **not** run: they call functions
  that no longer exist (`pre.typify`, `pre.trim`, `pre.merge_rows`) and reference absolute
  paths into repositories that are gone. Kept because they document how the figures were
  arrived at — `vldb.ipynb` in particular explains why the cactus plot was chosen.

## Known hazards

These do not affect the committed data but will bite when measurements are regenerated:

- **Sentinel drift.** `Measurement.java`'s `num()` now writes `""` for negative values, but
  the committed CSVs contain `-1`. New data will produce `NaN` where the Python expects
  `-1`, and `cactus_plot`'s `assert (df["candidates"] >= 0).all()` will pass vacuously
  instead of catching it.
- **Schema drift.** `80b8403f476d5b.csv` has an extra `num_sessions` column the other CSVs
  lack — the reason for the `if "num_sessions" in df` branch in `preprocessing.py`.
- **Embedded tab.** The `problem` field contains a literal tab inside an unquoted CSV field;
  any reformatting or round-trip of these files breaks `Problem.from_str`.
- `instances_solved` draws `brute_force` as a flat zero line even though no CSV contains it,
  and `order = [0, 3, 1, 4, 2]` in `plot1` reorders the legend positionally — both silently
  mislabel if the set of implementations changes.
- `preprocessing.py` silently drops rows: all `RA_c` problems, and `no_smart_search` for
  multi-framework UNSAT problems.
