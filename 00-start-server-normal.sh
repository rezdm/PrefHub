#!/bin/bash
cd "$(dirname "$0")"
export MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"

# Build the project
echo "Building PrefHub..."
mvn install -N
mvn clean package -DskipTests -pl prefhub-core,prefhub-server -am

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

# Set up classpath with all dependencies
CORE_JAR="prefhub-core/target/prefhub-core-1.0-SNAPSHOT.jar"
SERVER_JAR="prefhub-server/target/prefhub-server-1.0-SNAPSHOT.jar"
DEPS_DIR="prefhub-server/target/dependency"

# Download dependencies if needed
if [ ! -d "$DEPS_DIR" ]; then
    echo "Downloading dependencies..."
    mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -pl prefhub-server
fi

# Build classpath (server JAR + core JAR + all dependencies)
# Use the actual core JAR from target, not from dependencies (which may be outdated)
CLASSPATH="$SERVER_JAR:$CORE_JAR"
for jar in "$DEPS_DIR"/*.jar; do
    # Skip prefhub-core if it's in dependencies (we use the fresh one from target)
    if [[ "$jar" != *"prefhub-core"* ]]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# Copy rules files to game-data/rules
echo "Copying rules files..."
mkdir -p ./game-data/rules
cp -f prefhub-server/src/main/resources/rules/*.json ./game-data/rules/

echo "Starting PrefHub server..."
java -Djava.net.preferIPv4Stack=true \
    -cp "$CLASSPATH" \
    com.prefhub.server.ServerMain \
    8090 ./game-data

