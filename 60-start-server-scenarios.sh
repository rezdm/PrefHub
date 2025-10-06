#!/bin/bash
cd "$(dirname "$0")"

# Test the scenario mode

# Kill any existing server
lsof -ti:8090 | xargs -r kill -9 2>/dev/null
sleep 1

# Ensure dependencies are copied
echo "Ensuring dependencies are available..."
mvn dependency:copy-dependencies -DoutputDirectory=prefhub-server/target/dependency -pl prefhub-server -q

# Verify dependency directory exists
if [ ! -d "prefhub-server/target/dependency" ]; then
    echo "Error: Dependencies not found. Building project first..."
    mvn package -DskipTests -pl prefhub-core,prefhub-server -am
    mvn dependency:copy-dependencies -DoutputDirectory=prefhub-server/target/dependency -pl prefhub-server
fi

# Build the classpath
CORE_JAR="prefhub-core/target/prefhub-core-1.0-SNAPSHOT.jar"
SERVER_JAR="prefhub-server/target/prefhub-server-1.0-SNAPSHOT.jar"
DEPS_DIR="prefhub-server/target/dependency"

CLASSPATH="$SERVER_JAR:$CORE_JAR"
for jar in "$DEPS_DIR"/*.jar; do
    if [[ "$jar" != *"prefhub-core"* ]]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

echo "Classpath includes $(echo "$DEPS_DIR"/*.jar | wc -w) dependency JARs"
echo "Running scenario mode..."

# Run scenario mode
java -Djava.net.preferIPv4Stack=true \
    -cp "$CLASSPATH" \
    com.prefhub.server.ServerMain \
    --scenario test-scenarios.json \
    8090
