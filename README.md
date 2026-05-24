# Isolde - from "Verifying Isolation Level Specifications"

This project is a multi-module Maven project structured as follows:

- `core/` — framework-agnostic library for building history synthesis tools. Knows nothing about specific theoretical frameworks (Biswas, Cerone, ...).
- `cli/` — the Isolde tool: instantiations of specific frameworks (currently Biswas and Cerone) on top of `core`, plus the `isolde` command-line interface.
- `experiments/` — benchmarking and verification experiments, including the `isoldebench` CLI used to record solving times.

The state of this repository is work in progress. Deprecated code needs to be removed, and relevant code needs to be documented. However, the project can be tested and used.

## Examples

For examples on how to specify isolation levels, see the `haslab.isolde.cerone.definitions.CeroneDefinitions` and `haslab.isolde.biswas.definitions.AxiomaticDefinitions` classes in the `cli` module.

For examples on how to use Isolde programmatically, see the `experiments` module, which contains all the code used to run the experiments mentioned in the paper.

## Building

    mvn package

This produces `cli/target/isolde.jar` (the synthesis CLI) and `experiments/target/isoldebench.jar` (the benchmarking CLI).

## Running the CLI

    java -jar cli/target/isolde.jar --txn 3 --obj 2 --val 2 --require biswas:Ser
