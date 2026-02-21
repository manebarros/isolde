#!/usr/bin/env bash
set -euo pipefail

if ! git diff-index --quiet HEAD --; then
    echo "Repository is not clean."
    exit 1
fi

# Get the commit hash that HEAD points to (14-char short hash)
COMMIT_HASH=$(git rev-parse --short=14 HEAD)

CSV_PATH="isolde-experiments/data/${COMMIT_HASH}.csv"

for impl in all no_smart_search no_fixed_co no_learning
do
    mvn clean package && \
    java -Xms4g -Xmx16g \
    -jar isolde-experiments/target/isolde-experiments-1.0-SNAPSHOT.jar \
    "$CSV_PATH" \
    --txn 3 \
    --obj 5 \
    --val 5 \
    --solvers glucose \
    --timeout 3600 \
    --impl "$impl"
done
