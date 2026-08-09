# Isolde

Isolde reasons about **transactional isolation level specifications**. Given two specifications *A*
and *B*, it automatically synthesizes a transactional history that is allowed by *A* and disallowed
by *B*, searching every history within a bounded scope (a number of transactions, objects and
values). It does so with a CEGIS loop on top of [Kodkod](https://github.com/emina/kodkod) and an
off-the-shelf SAT solver.

Isolde is the tool behind *Reasoning about Transactional Isolation Levels with Isolde* (Barros, Kang,
Cunha, Pereira). [Reproducing the paper](#reproducing-the-paper) maps each of the paper's results
onto a command.

## Requirements

- **JDK 21**. The build targets Java 21 (`maven.compiler.release`).
- **Maven** — or just use the bundled `./mvnw`, which pins the version the project is built with.
- **[uv](https://docs.astral.sh/uv/)**, only to build the paper's plots.

## Setting up

Isolde is built on [Kodkod](https://github.com/emina/kodkod), which is not published to Maven
Central or any other public repository. It has to be placed in your local Maven repository before
the build can resolve it, which is what this does:

    scripts/setup-kodkod.sh

The script downloads Kodkod 2.1 from its upstream GitHub release, checks it against a pinned
SHA-256, and installs it as `com.github.emina:kodkod:2.1`. Run it once per machine; re-running is
harmless. If a download does not match its expected checksum the script installs nothing and stops.

It also fetches the **native SAT solvers**, which are a separate download and are *not* part of the
Kodkod jar. `--solver minisat` and `--solver glucose` are considerably faster than the default, but
they are JNI libraries that have to be on the library path:

    java -Djava.library.path=lib/native/linux_x86_64 -jar cli/target/isolde.jar ...

Without them you get an `UnsatisfiedLinkError`. The default `sat4j` is pure Java and always works.
Upstream only ships prebuilt libraries for **x86-64 Linux and macOS** — on Apple Silicon or ARM
Linux, `sat4j` is the only option, which also means the paper's benchmark cannot be reproduced
as-published there (it runs Glucose).

## Repository layout

| directory | contents |
|---|---|
| `core/` | The synthesis engine. Framework-agnostic: it knows about histories, executions and the CEGIS loop, but nothing about any particular isolation framework. |
| `cli/` | Instantiations of the Biswas–Enea and Cerone frameworks on top of `core`, the catalogs of isolation-level definitions, and the `isolde` command-line interface. |
| `experiments/` | The `isoldebench` benchmarking CLI and the paper's verification case studies. |
| `analysis/` | Python scripts that turn an `isoldebench` CSV into the paper's figures. See [`analysis/README.md`](analysis/README.md). |
| `benchmark_latest.sh` | Runs the paper's full benchmark against the current commit. |

## Building

    ./mvnw package

This produces two executable jars:

- `cli/target/isolde.jar` — the synthesis CLI
- `experiments/target/isoldebench.jar` — the benchmarking CLI

The examples below assume a shorthand:

    alias isolde='java -jar '"$PWD"'/cli/target/isolde.jar'

Run the tests with `./mvnw test`.

Sources are formatted with [google-java-format](https://github.com/google/google-java-format), and
the build enforces it: an unformatted tree fails in `validate`, before anything is compiled. To fix,

    ./mvnw spotless:apply

### Reproducibility

The build is pinned end to end, so the same commit produces the same artifacts anywhere: the JDK
release, the Maven version (via `./mvnw`), every plugin version, every dependency version, the
source encoding, and the Kodkod jar by SHA-256. `project.build.outputTimestamp` keeps build times
out of the jars, so two builds of one commit are byte-identical — which matters because benchmark
data files are named after the commit that produced them.

## Solving synthesis problems

    isolde [--txn N] [--obj N] [--val N] [--solver S] --require FRAMEWORK:LEVEL ... --forbid FRAMEWORK:LEVEL ...

| option | meaning |
|---|---|
| `--txn`, `--obj`, `--val` | The scope: how many transactions, objects and values the synthesized history may use. Each defaults to 3. |
| `--require` | The history must be allowed by this level. Repeatable — each occurrence is a separate requirement. |
| `--forbid` | The history must be disallowed by this level. Repeatable. |
| `--solver` | `sat4j` (default), `minisat` or `glucose`. |

At least one of `--require`/`--forbid` is needed. Exit codes: **0** a history was found, **1** no
history exists within the scope, **2** the arguments were rejected.

### Available levels

`isolde levels` prints the catalog, one prefix per framework:

| prefix | definitions |
|---|---|
| `biswas:` | The framework of Biswas and Enea. Its axiomatic definitions: `ReadAtomic`, `Causal`, `Prefix`, `Conflict`, `Snapshot`, `Ser`, `UpdateSer`. And Plume's characterization of isolation levels by *Transactional Anomalous Patterns*: `TapReadAtomic`, `TapCausal`. |
| `cerone:` | The framework of Cerone et al.: the levels `RA`, `CC`, `UA`, `PSI`, `PC`, `SI`, `Ser`, `UpdateSer`, `NLU`, and the individual axioms they are built from (`EXT`, `NO_CONF`, `TRANS_VIS`, `PREFIX`, `TOTAL_VIS`, `SESSION`), which can be required or forbidden on their own. `isolde levels` also lists a few exploratory definitions from `CustomDefinitions`. |

## Comparing isolation level definitions

`isolde compare` synthesizes in both directions and classifies the result:

    isolde compare biswas:Causal biswas:Ser --txn 3 --obj 2 --val 2

```
Comparing biswas:Causal and biswas:Ser with scope: 3 transactions, 2 objects, 2 values

biswas:Ser is STRONGER than biswas:Causal.
History allowed by biswas:Causal but not by biswas:Ser:
1: r(1,0) w(1,1)
|
2: r(0,0)

3: r(0,0) r(1,0) w(0,1)

Execution #1:
Commit Order:
1 -> 2 -> 3

Using 3 transactions, 2 objects, 2 values.
Time for synthesizing biswas:Causal and not biswas:Ser: 156 ms.
Time for synthesizing biswas:Ser and not biswas:Causal: 16 ms.
Total comparison time: 172 ms.
```

The four verdicts:

| verdict | meaning |
|---|---|
| `A is STRONGER than B` | Nothing allowed by *A* is disallowed by *B*, but not the reverse. The printed history is allowed by *B* only. |
| `B is STRONGER than A` | The mirror image. |
| `INCOMPARABLE` | Each allows a history the other rejects. Both are printed. |
| `EQUIVALENT` | Neither direction produced a history. |

**`EQUIVALENT` is not a proof.** It means no history *within this scope* distinguishes the two
definitions. Re-run at a larger scope for more confidence; the paper's own equivalence results use
five transactions, five objects and five values.

The comparison works across prefixes, which is how definitions in different frameworks are checked
against each other:

    $ isolde compare biswas:Snapshot cerone:SI --txn 3 --obj 2 --val 2
    biswas:Snapshot and cerone:SI are EQUIVALENT.

## Specifying a new isolation level

Definitions live in `cli/src/main/java/haslab/isolde/biswas/definitions/` and
`cli/src/main/java/haslab/isolde/cerone/definitions/`, and are written against Isolde's Java API.

### The shape of a definition

A definition is an `ExecutionFormula<E>` — a function from an execution to a Kodkod formula:

```java
@FunctionalInterface
public interface ExecutionFormula<E extends Execution> {
  Formula resolve(E execution);
}
```

`E` says which framework the definition belongs to, and therefore what structure is available
besides the history:

| execution type | components |
|---|---|
| `BiswasExecution` | `history()`, `co()` — a commit order |
| `CeroneExecution` | `history()`, `vis()`, `ar()` — visibility and arbitration |

A history *satisfies* a definition when **some** execution over it satisfies the formula; the
existential quantification over `co`/`vis`/`ar` is Isolde's job, not yours. The framework's baseline
axioms are built into the encoders, so a definition only states what its level adds: in Cerone's
framework `EXT`, `SESSION`, `vis ⊆ ar` and totality of `ar` always hold, which is why
`CeroneDefinitions.RA` is simply `Formula.TRUE`.

Definitions compose with `and`, `implies` and `not`, so a level is usually assembled from named
axioms:

```java
public static final ExecutionFormula<CeroneExecution> SI = PREFIX.and(NO_CONF);
```

### The history vocabulary

Formula bodies are written against `HistorySchema`
(`core/src/main/java/haslab/isolde/core/HistorySchema.java`), which exposes the history as Kodkod
relations. The most useful members:

| member | meaning |
|---|---|
| `transactions()`, `normalTxns()`, `initialTransaction()` | All transactions; all but the initial one; the initial one. |
| `keys()`, `values()` | The objects and the values in scope. |
| `externalRead(t, x, n)` | `t` reads value `n` of object `x`, written by another transaction. |
| `finalWrite(t, x, n)` | `n` is the last value `t` writes to `x`. |
| `wr(t, x, s)`, `wr()`, `binaryWr()` | `s` reads `x` from `t`; the same as a relation. |
| `writes(t, x)`, `reads(t, x)` | `t` writes to / reads from `x`. |
| `sessionOrder()`, `causalOrder()`, `causallyOrdered(t, s)` | Session order; its union with `wr`, transitively closed. |
| `txnThatWriteToAnyOf(x)`, `txnThatReadAnyOf(x)` | The transactions that write / read `x`. |
| `updateTransactions()`, `isUpdateTransaction(t)` | The transactions that write something. |
| `initialValue(x)`, `valuesWrittenTo(x)`, `writerOf(x, v)` | Values and their writers. |
| `subHistory(...)`, `projectionOverKeys(...)` | Restrict a definition to a subset of the transactions or of the objects. |

Everything else is plain Kodkod: `Variable`, `Formula.and`, `product`, `join`, `in`, `forAll`,
`forSome`, `closure`, `comprehension`.

### Example: UpdateSer

`UpdateSer` is Serializability with read-only transactions exempted. It can be written as
Serializability's implication with one extra premise — that the reading transaction performs a write
(`AxiomaticDefinitions.UpdateSerExplicit`):

```java
public static Formula UpdateSerExplicit(BiswasExecution e) {
  Variable t1 = Variable.unary("t1");
  Variable t2 = Variable.unary("t2");
  Variable t3 = Variable.unary("t3");
  Variable x = Variable.unary("x");

  return Formula.and(t1.eq(t2).not(), e.history().wr(t1, x, t3), t2.product(t3).in(e.co()))
      .implies(t1.in(t2.join(e.co())))
      .forAll(
          x.oneOf(e.history().keys())
              .and(
                  t1.oneOf(e.history().txnThatWriteToAnyOf(x))
                      .and(
                          t2.oneOf(e.history().txnThatWriteToAnyOf(x))
                              .and(t3.oneOf(e.history().updateTransactions())))));
}
```

The same level can be had by restricting the whole of Serializability to the update sub-history,
which is how `AxiomaticDefinitions.UpdateSer` is written:

```java
public static Formula UpdateSer(BiswasExecution e) {
  return Serializability(
      new BiswasExecution(e.history().subHistory(HistorySchema::updateTransactions), e.co()));
}
```

Two formulations of the same intent is exactly the situation Isolde is for, and
`isolde compare biswas:UpdateSerExplicit biswas:UpdateSer` reports them equivalent.

### Solving with a new definition

Wrap definitions as constraints, combine them into a spec, and hand it to a synthesizer:

```java
IsoldeSpec spec =
    IsoldeConstraint.biswas(AxiomaticDefinitions.Snapshot)
        .and(IsoldeConstraint.biswas(AxiomaticDefinitions.UpdateSer))
        .andNot(IsoldeConstraint.biswas(AxiomaticDefinitions.Ser))
        .build();

Scope scope = new Scope.Builder().txn(3).obj(2).val(2).build();
SynthesizedHistory result =
    new IsoldeSynthesizer.Builder().solver(SATFactory.MiniSat).build().synthesize(scope, spec);

if (result.sat()) System.out.println(result.history());
```

`IsoldeConstraint.biswas`, `.cerone` and `.history` build the three kinds of constraint, and
`IsoldeSpec.Builder` combines them: `and` adds a requirement — each getting its own existentially
quantified execution — while `andNot` conjoins into the single "disallowed by" side. From the result,
`sat()`, `history()`, `time()` and `candidates()` are the interesting accessors, and `toString()`
renders the history with its executions the way the CLI does. The `Synthesizer` /
`SynthesisSpec.allowedBy(...).andDisallowedBy(...)` pair used throughout `experiments/` is a
shorthand for the same thing.

## Reproducing the paper

### Case studies (Section 3)

Each of the paper's qualitative results is a single command. The results below are what these
commands print; scopes are chosen to be quick to re-run rather than to match the paper exactly.

| paper | command | result |
|---|---|---|
| §3.1.1 Plume's `RA` is **not** equivalent to the axiomatic `RA` | `isolde compare biswas:TapReadAtomic biswas:ReadAtomic --txn 2 --obj 1 --val 2` | `biswas:ReadAtomic is STRONGER`, with the paper's minimal two-transaction witness |
| §3.1.2 equivalence across the two frameworks | `isolde compare cerone:RA biswas:ReadAtomic`, and likewise `cerone:CC`/`biswas:Causal`, `cerone:PC`/`biswas:Prefix`, `cerone:SI`/`biswas:Snapshot`, `cerone:Ser`/`biswas:Ser`, `cerone:UpdateSer`/`biswas:UpdateSer` | `EQUIVALENT` |
| §3.2.1 the read-only anomaly under Snapshot Isolation | `isolde --txn 3 --obj 2 --val 2 --require biswas:Snapshot --require biswas:UpdateSer --forbid biswas:Ser` | a history, with the two disagreeing commit orders |
| §3.2.1 the same, in Cerone's framework | the same with `cerone:SI`, `cerone:UpdateSer`, `--forbid cerone:Ser` | a history, with two `Vis`/`Ar` pairs |
| §3.2.1 the two `UpdateSer` formulations agree | `isolde compare biswas:UpdateSerExplicit biswas:UpdateSer` | `EQUIVALENT` |
| §3.2.2 Update Atomic is strictly stronger than "no lost updates" | `isolde compare cerone:UA cerone:NLU --txn 3 --obj 2 --val 4` | `cerone:UA is STRONGER`, with the paper's Figure 1 witness |

Two notes. The paper's equivalence claims use a scope of five transactions, objects and values
(`--txn 5 --obj 5 --val 5`); those runs are much slower than the defaults above, and Section 5
quantifies how much. And `FeketeReadOnlyAnomaly.generateAnomalyCerone` additionally constrains the
*shape* of the history — one transaction per session — which is a `HistoryFormula` and so not
expressible on the command line; that variant stays an API-level example.

The corresponding programmatic entry points are in
`experiments/src/main/java/haslab/isolde/experiments/verification/`: `VerifyPlumeDefinitions`,
`VerifyBiswasAndCeroneEquivalence`, `VerifyUpdateSerDefinitions` and `FeketeReadOnlyAnomaly`. They
sweep several definitions per run and can be called from `jshell` as shown above.

### The benchmark (Section 5)

The full benchmark behind Figures 3 and 4:

    ./benchmark_latest.sh

It refuses to run on a dirty working tree, names its output
`experiments/data/<14-char commit hash>.csv`, and for each of the four implementations (`all`,
`no_smart_search`, `no_fixed_co`, `no_learning`) rebuilds the project and runs every benchmark
problem over 3–10 transactions with 5 objects and 5 values, using Glucose, a one-hour timeout and a
16 GB heap. Because the CSV is named after the commit, its file name identifies
the version of Isolde that produced the measurements.

It runs Glucose, so the [native solvers](#setting-up) have to be installed and on the library path;
otherwise every run fails with an `UnsatisfiedLinkError`.

For anything smaller, drive `isoldebench` directly:

    java -jar experiments/target/isoldebench.jar OUT.csv [options]

| option | meaning |
|---|---|
| `OUT.csv` | Destination. Rows are appended; the header is written only when the file is new. |
| `--txn`, `--obj`, `--val` | `N` or a range `start:end`. Each defaults to 3. |
| `--classes` | `SAT_SAME`, `SAT_DIFF`, `UNSAT_SAME`, `UNSAT_DIFF` — the four cells of the paper's Table 1: satisfiable or not, one framework or two. Defaults to all four. |
| `-s`, `--single` | One representative problem per class instead of the whole set. The quick smoke test. |
| `--impl` | `all` (Isolde as published), `no_smart_search`, `no_fixed_co`, `no_incremental`, `none`, `no_learning`, `exhaustive`. Defaults to every CEGIS variant. |
| `--solvers` | `sat4j`, `minisat`, `glucose`. |
| `--timeout` | Per-run timeout in seconds (default 300). |

A quick run to check the pipeline works end to end:

    java -jar experiments/target/isoldebench.jar /tmp/smoke.csv -s \
        --impl all --solvers sat4j --timeout 60

The measurements used in the paper are committed under `experiments/data/`; `d8dfd9f4814950.csv` is
the one its figures were built from.

### The plots

    cd analysis
    uv run plots.py --list                       # what can be built
    uv run plots.py                              # build every figure
    uv run plots.py cactus_runtime               # build one
    uv run plots.py --data ../experiments/data/3c508f8a48d334.csv

`uv` creates the environment on first run. Each figure is written as `.pgf` (what the paper
`\input`s) and `.pdf` (for previewing) under `analysis/plots/<csv name>/`, so results from different
measurement runs never overwrite each other.

[`analysis/README.md`](analysis/README.md) has the figure-to-paper-figure table, explains how a plot
is traced back to the data and tool version it came from, and lists the hazards to watch for when
regenerating measurements.
