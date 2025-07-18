# Isolde - from "Verifying Isolation Level Specifications"

This project consists in a multi-module maven project. Isolde is a java library that exposes several methods to synthesize database histories conforming to isolation specifications. 

The state of this repository is work in progress. Deprecated code needs to be removed, and relevant code needs to be documented. However, the project can to be tested and used.

Isolde lives in the `isolde-core` module. For examples on how to specify isolation levels, see, for instance, the `haslab.isolde.cerone.definitions.CeroneDefinitions` and `haslab.isolde.biswas.definitions.AxiomaticDefinitions` classes. 

For examples on how to use Isolde, see the `isolde-experiments` module, which contains all the code used to run the experiments mentioned in the paper.
