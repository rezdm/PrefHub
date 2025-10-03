#!/bin/bash

# Script to automatically set up a test game with 3 players
# This script registers 3 users and creates/joins a game

PREFHUB_URL="${PREFHUB_URL:-http://localhost:8090}"
GAME_ID="${GAME_ID:-test-game-$(date +%s)}"
RULES="${RULES:-leningradka}"

echo "=========================================="
echo "PrefHub Test Game Setup"
echo "=========================================="
echo "Server: $PREFHUB_URL"
echo "Game ID: $GAME_ID"
echo "Rules: $RULES"
echo ""

# Function to register a user
register_user() {
    local username=$1
    local password=$2

    echo "Registering user: $username"

    response=$(curl -s -X POST "$PREFHUB_URL/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")

    if echo "$response" | grep -q "error"; then
        echo "  ⚠ Registration failed (user may already exist): $(echo $response | jq -r '.error' 2>/dev/null || echo $response)"
    else
        echo "  ✓ Registered successfully"
    fi
}

# Function to login a user
login_user() {
    local username=$1
    local password=$2

    echo "Logging in: $username"

    response=$(curl -s -X POST "$PREFHUB_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")

    token=$(echo "$response" | jq -r '.token' 2>/dev/null)

    if [ "$token" = "null" ] || [ -z "$token" ]; then
        echo "  ✗ Login failed: $(echo $response | jq -r '.error' 2>/dev/null || echo $response)"
        return 1
    else
        echo "  ✓ Logged in successfully"
        echo "$token"
        return 0
    fi
}

# Function to create a game
create_game() {
    local token=$1
    local game_id=$2
    local rule_id=$3

    echo "Creating game: $game_id with rules: $rule_id"

    response=$(curl -s -X POST "$PREFHUB_URL/api/games/create" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "{\"gameId\":\"$game_id\",\"ruleId\":\"$rule_id\"}")

    if echo "$response" | grep -q "error"; then
        echo "  ✗ Failed to create game: $(echo $response | jq -r '.error' 2>/dev/null || echo $response)"
        return 1
    else
        echo "  ✓ Game created successfully"
        return 0
    fi
}

# Function to join a game
join_game() {
    local token=$1
    local game_id=$2
    local username=$3

    echo "Player $username joining game: $game_id"

    response=$(curl -s -X POST "$PREFHUB_URL/api/games/join" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "{\"gameId\":\"$game_id\"}")

    if echo "$response" | grep -q "error"; then
        echo "  ✗ Failed to join game: $(echo $response | jq -r '.error' 2>/dev/null || echo $response)"
        return 1
    else
        echo "  ✓ Joined successfully"
        return 0
    fi
}

# Check if server is running
echo "Checking if PrefHub server is running..."
if ! curl -s "$PREFHUB_URL" > /dev/null; then
    echo "✗ Cannot connect to $PREFHUB_URL"
    echo "Please start the server first: ./start-server.sh"
    exit 1
fi
echo "✓ Server is running"
echo ""

# Register and setup players
PLAYERS=("alice" "bob" "charlie")
TOKENS=()

echo "Step 1: Registering players..."
echo "----------------------------------------"
for player in "${PLAYERS[@]}"; do
    register_user "$player" "password123"
done
echo ""

echo "Step 2: Logging in players..."
echo "----------------------------------------"
for player in "${PLAYERS[@]}"; do
    token=$(login_user "$player" "password123")
    if [ $? -eq 0 ]; then
        TOKENS+=("$token")
    else
        echo "Failed to login $player"
        exit 1
    fi
done
echo ""

echo "Step 3: Creating game..."
echo "----------------------------------------"
create_game "${TOKENS[0]}" "$GAME_ID" "$RULES"
if [ $? -ne 0 ]; then
    echo "Failed to create game"
    exit 1
fi
echo ""

echo "Step 4: Joining game..."
echo "----------------------------------------"
# Player 2 and 3 join the game (player 1 already in as creator)
for i in 1 2; do
    join_game "${TOKENS[$i]}" "$GAME_ID" "${PLAYERS[$i]}"
    if [ $? -ne 0 ]; then
        echo "Failed to join game with ${PLAYERS[$i]}"
        exit 1
    fi
    sleep 0.5
done
echo ""

echo "=========================================="
echo "✓ Test game setup complete!"
echo "=========================================="
echo ""
echo "Game details:"
echo "  Game ID: $GAME_ID"
echo "  Rules: $RULES"
echo "  Players:"
for i in "${!PLAYERS[@]}"; do
    echo "    - ${PLAYERS[$i]}"
done
echo ""
echo "You can now open browsers and login with:"
for player in "${PLAYERS[@]}"; do
    echo "  Username: $player, Password: password123"
done
echo ""
echo "Or run: ./start-test-clients.sh"
echo "Then manually login each client with the credentials above"
