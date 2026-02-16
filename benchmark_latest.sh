#!/usr/bin/env bash
set -euo pipefail

# Check if staging area is clean
if ! git diff-index --quiet --cached HEAD; then
  echo "Error: Staging area is not clean. Please commit or unstage changes before running."
  exit 1
fi

# Get the commit hash that HEAD points to (14-char short hash)
COMMIT_HASH=$(git rev-parse --short=14 HEAD)

CSV_PATH="isolde-experiments/data/${COMMIT_HASH}.csv"

mvn clean package && \
java -Xms2g -Xmx8g \
  -jar isolde-experiments/target/isolde-experiments-1.0-SNAPSHOT.jar \
  "$CSV_PATH" \
  --txn 3:7 \
  --obj 5 \
  --val 5 \
  --sess 3 \
  --solvers glucose \
  --impl all,no_smart_search,no_fixed_co,no_incremental,no_learning
