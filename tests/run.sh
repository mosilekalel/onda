#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/.."
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
javac -d "$OUT" app/src/main/java/br/com/meshlivre/Packet.java app/src/main/java/br/com/meshlivre/Router.java tests/ProtocolTest.java
java -cp "$OUT" ProtocolTest
