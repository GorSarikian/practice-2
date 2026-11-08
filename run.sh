#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
find src -name '*.java' > build/sources.txt
if [ "${1:-}" = "test" ]; then
    find tests -name '*.java' >> build/sources.txt
    javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
    exec java -Dfile.encoding=UTF-8 -cp build/classes nugetgraph.AllTests
fi
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
exec java -Dfile.encoding=UTF-8 -cp build/classes nugetgraph.Main "$@"
