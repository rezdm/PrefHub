#!/bin/bash
# Common server startup functions
# Source this script from other server startup scripts

# Navigate to project root
cd "$(dirname "$0")"
export MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"

# Function to build the project
build_project() {
    echo "Building PrefHub..."
    mvn install -N
    mvn clean package -DskipTests -pl prefhub-core,prefhub-server -am

    if [ $? -ne 0 ]; then
        echo "Build failed!"
        exit 1
    fi
}

# Function to set up classpath
setup_classpath() {
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

    # Export for use in calling script
    export CLASSPATH
}

# Function to copy rules files
copy_rules() {
    echo "Copying rules files..."
    mkdir -p ./game-data/rules
    cp -f prefhub-server/src/main/resources/rules/*.json ./game-data/rules/
}

# Function to start server with given arguments
start_server() {
    echo "Starting PrefHub server..."
    java -Djava.net.preferIPv4Stack=true \
        -cp "$CLASSPATH" \
        com.prefhub.server.ServerMain \
        "$@"
}
