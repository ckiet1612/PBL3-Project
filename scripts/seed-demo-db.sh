#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

CP_FILE="${TMPDIR:-/tmp}/pbl3_demo_seed_cp.txt"

./mvnw -q -DskipTests compile dependency:build-classpath -Dmdep.outputFile="$CP_FILE"
java -cp "target/classes:$(cat "$CP_FILE")" com.pbl3.project.pbl3_project.DemoSeedApplication
