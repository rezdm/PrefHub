#!/bin/bash

# Health check script for PrefHub testing environment

echo "=========================================="
echo "PrefHub Testing Environment Health Check"
echo "=========================================="
echo ""

PREFHUB_URL="${PREFHUB_URL:-http://localhost:8090}"
EXIT_CODE=0

# Function to check a requirement
check_requirement() {
    local name=$1
    local command=$2
    local install_hint=$3

    printf "%-30s" "Checking $name..."
    if eval "$command" &> /dev/null; then
        echo "✓ OK"
        return 0
    else
        echo "✗ MISSING"
        if [ -n "$install_hint" ]; then
            echo "  Install: $install_hint"
        fi
        EXIT_CODE=1
        return 1
    fi
}

# Check requirements
echo "Requirements:"
echo "----------------------------------------"
check_requirement "Chrome/Chromium" "command -v google-chrome || command -v chromium || command -v chromium-browser || [ -f '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' ]" "sudo apt install chromium-browser"
check_requirement "curl" "command -v curl" "sudo apt install curl"
check_requirement "jq" "command -v jq" "sudo apt install jq"
check_requirement "Java (for server)" "command -v java" "sudo apt install openjdk-21-jdk"
check_requirement "Maven (for build)" "command -v mvn" "sudo apt install maven"

echo ""

# Check if server is built
echo "Build Status:"
echo "----------------------------------------"
if [ -f "prefhub-server/target/prefhub-server-1.0-SNAPSHOT.jar" ]; then
    echo "Server JAR:                     ✓ Built"
else
    echo "Server JAR:                     ✗ Not found"
    echo "  Run: mvn clean package"
    EXIT_CODE=1
fi

if [ -f "prefhub-core/target/prefhub-core-1.0-SNAPSHOT.jar" ]; then
    echo "Core JAR:                       ✓ Built"
else
    echo "Core JAR:                       ✗ Not found"
    echo "  Run: mvn clean package"
    EXIT_CODE=1
fi

echo ""

# Check if rules are present
echo "Game Rules:"
echo "----------------------------------------"
RULES_DIR="prefhub-server/src/main/resources/rules"
if [ -d "$RULES_DIR" ]; then
    rule_count=$(find "$RULES_DIR" -name "*.json" | wc -l)
    if [ "$rule_count" -gt 0 ]; then
        echo "Rules files:                    ✓ Found $rule_count variants"
        for rule in "$RULES_DIR"/*.json; do
            if [ -f "$rule" ]; then
                rule_name=$(basename "$rule" .json)
                echo "  - $rule_name"
            fi
        done
    else
        echo "Rules files:                    ✗ No rules found"
        EXIT_CODE=1
    fi
else
    echo "Rules directory:                ✗ Not found"
    EXIT_CODE=1
fi

echo ""

# Check if server is running
echo "Server Status:"
echo "----------------------------------------"
printf "%-30s" "Server connectivity..."
if curl -s --max-time 3 "$PREFHUB_URL" > /dev/null 2>&1; then
    echo "✓ Running at $PREFHUB_URL"

    # Check API endpoints
    printf "%-30s" "Rules API..."
    if curl -s --max-time 3 "$PREFHUB_URL/api/rules/list" > /dev/null 2>&1; then
        echo "✓ Accessible"
    else
        echo "✗ Not accessible"
        EXIT_CODE=1
    fi
else
    echo "✗ Not running"
    echo ""
    echo "  To start the server:"
    echo "    ./start-server.sh"
    echo ""
    EXIT_CODE=1
fi

echo ""

# Check test scripts
echo "Test Scripts:"
echo "----------------------------------------"
if [ -x "./start-test-clients.sh" ]; then
    echo "start-test-clients.sh:          ✓ Ready"
else
    echo "start-test-clients.sh:          ✗ Not executable"
    echo "  Run: chmod +x start-test-clients.sh"
    EXIT_CODE=1
fi

if [ -x "./setup-test-game.sh" ]; then
    echo "setup-test-game.sh:             ✓ Ready"
else
    echo "setup-test-game.sh:             ✗ Not executable"
    echo "  Run: chmod +x setup-test-game.sh"
    EXIT_CODE=1
fi

if [ -x "./start-server.sh" ]; then
    echo "start-server.sh:                ✓ Ready"
else
    echo "start-server.sh:                ✗ Not executable"
    echo "  Run: chmod +x start-server.sh"
    EXIT_CODE=1
fi

echo ""
echo "=========================================="

if [ $EXIT_CODE -eq 0 ]; then
    echo "✓ All checks passed!"
    echo "=========================================="
    echo ""
    echo "Ready to test! Run:"
    echo "  ./setup-test-game.sh    # Setup users and game"
    echo "  ./start-test-clients.sh # Start browser clients"
else
    echo "✗ Some checks failed"
    echo "=========================================="
    echo ""
    echo "Please fix the issues above before testing."
fi

echo ""

exit $EXIT_CODE
