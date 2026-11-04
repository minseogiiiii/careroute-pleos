#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/core-checks
javac -encoding UTF-8 -d build/core-checks app/src/main/java/com/careroute/firstround/JourneyEngine.java app/src/main/java/com/careroute/firstround/SessionCodec.java tests/EngineChecks.java
java -cp build/core-checks com.careroute.firstround.EngineChecks tests/web-parity.tsv
