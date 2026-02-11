#!/usr/bin/env bash
set -euo pipefail

# Get the commit hash that main points to (14-char short hash)
COMMIT_HASH=$(git rev-parse --short=14 main)

CSV_PATH="isolde-experiments/data/${COMMIT_HASH}.csv"

mvn clean package && \
java -Xms2g -Xmx8g \
  -jar isolde-experiments/target/isolde-experiments-1.0-SNAPSHOT.jar \
  "$CSV_PATH" \
  --txn 3:7 \
  --obj 5 \
  --val 5 \
  --sess 3 \
  --solvers minisat,glucose
