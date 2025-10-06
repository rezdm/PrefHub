#!/bin/bash

# Quick test script - creates game and launches authenticated React Chrome instances
# Opens multiple Chrome windows with React frontend (port 3000) pre-authenticated

REACT_URL="${REACT_URL:-http://localhost:3000}"
API_URL="${API_URL:-http://localhost:8090}"
TIMESTAMP=$(date +%s)
GAME_ID="game-${TIMESTAMP}"
RULES="${RULES:-leningradka}"
CHROME_BIN=""

# Detect Chrome binary
if command -v google-chrome &> /dev/null; then
    CHROME_BIN="google-chrome"
elif command -v chromium &> /dev/null; then
    CHROME_BIN="chromium"
elif command -v chromium-browser &> /dev/null; then
    CHROME_BIN="chromium-browser"
elif [ -f "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" ]; then
    CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
else
    echo "Error: Chrome or Chromium not found!"
    exit 1
fi

echo "=========================================="
echo "PrefHub React Frontend Quick Test Setup"
echo "=========================================="
echo "React Frontend: $REACT_URL"
echo "API Server: $API_URL"
echo "Game ID: $GAME_ID"
echo "Rules: $RULES"
echo ""

# Generate random player names
PLAYER1="player${TIMESTAMP}a"
PLAYER2="player${TIMESTAMP}b"
PLAYER3="player${TIMESTAMP}c"
PASSWORD="test123"

PLAYERS=("$PLAYER1" "$PLAYER2" "$PLAYER3")
TOKENS=()

# Function to register a user
register_user() {
    local username=$1
    local password=$2

    echo "Registering user: $username"

    response=$(curl -s -X POST "$API_URL/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")

    if echo "$response" | grep -q "error"; then
        echo "  ⚠ Registration failed: $(echo $response | jq -r '.error' 2>/dev/null || echo $response)"
    else
        echo "  ✓ Registered successfully"
    fi
}

# Function to login a user
login_user() {
    local username=$1
    local password=$2

    echo "Logging in: $username" >&2

    response=$(curl -s -X POST "$API_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")

    token=$(echo "$response" | jq -r '.token' 2>/dev/null)

    if [ "$token" = "null" ] || [ -z "$token" ]; then
        echo "  ✗ Login failed" >&2
        return 1
    else
        echo "  ✓ Logged in successfully" >&2
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

    response=$(curl -s -X POST "$API_URL/api/games/create" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "{\"gameId\":\"$game_id\",\"ruleId\":\"$rule_id\"}")

    if echo "$response" | grep -q "error"; then
        echo "  ✗ Failed to create game"
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

    response=$(curl -s -X POST "$API_URL/api/games/join" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "{\"gameId\":\"$game_id\"}")

    if echo "$response" | grep -q "error"; then
        echo "  ✗ Failed to join game"
        return 1
    else
        echo "  ✓ Joined successfully"
        return 0
    fi
}

# Check if React dev server is running
echo "Checking if React dev server is running..."
if ! curl -s "$REACT_URL" > /dev/null; then
    echo "✗ Cannot connect to $REACT_URL"
    echo "Please start the React dev server first: ./30-start-frontend.sh"
    exit 1
fi
echo "✓ React dev server is running"
echo ""

# Check if API server is running
echo "Checking if API server is running..."
if ! curl -s "$API_URL" > /dev/null; then
    echo "✗ Cannot connect to $API_URL"
    echo "Please start the backend server first: ./10-start-server-normal.sh"
    exit 1
fi
echo "✓ API server is running"
echo ""

# Register and setup players
echo "Step 1: Registering players..."
echo "----------------------------------------"
for player in "${PLAYERS[@]}"; do
    register_user "$player" "$PASSWORD"
done
echo ""

echo "Step 2: Logging in players..."
echo "----------------------------------------"
for player in "${PLAYERS[@]}"; do
    token=$(login_user "$player" "$PASSWORD")
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

echo "Step 4: All players joining game..."
echo "----------------------------------------"
# Player 1 already created the game, so players 2 and 3 need to join
for i in 1 2; do
    join_game "${TOKENS[$i]}" "$GAME_ID" "${PLAYERS[$i]}"
    if [ $? -ne 0 ]; then
        echo "Failed to join game with ${PLAYERS[$i]}"
        exit 1
    fi
    sleep 0.5
done
echo "✓ All 3 players are now in game $GAME_ID"
echo ""

echo "Step 5: Preparing Chrome setup..."
echo "----------------------------------------"

# Create temp directory for user data
TEMP_DIR=$(mktemp -d -t prefhub-react-test-XXXXXX)
echo "Temp directory: $TEMP_DIR"
echo ""

echo "Step 6: Starting Chrome instances..."
echo "----------------------------------------"

# Function to start a Chrome instance with pre-authenticated session
start_chrome_authenticated() {
    local player_num=$1
    local port=$((9222 + player_num))
    local user_dir="$TEMP_DIR/player$player_num"
    local username="${PLAYERS[$player_num - 1]}"
    local token="${TOKENS[$player_num - 1]}"

    mkdir -p "$user_dir/Default"

    # Create preferences to disable password manager and translation
    cat > "$user_dir/Default/Preferences" << 'EOFPREF'
{
  "credentials_enable_service": false,
  "profile": {
    "password_manager_enabled": false,
    "password_manager_leak_detection": false
  },
  "translate": {
    "enabled": false
  },
  "translate_blocked_languages": ["ru"]
}
EOFPREF

    # Create a simple HTML page that opens React app with auth
    local launcher_file="$user_dir/launcher.html"
    cat > "$launcher_file" << EOFLAUNCHER
<!DOCTYPE html>
<html>
<head>
    <title>Launching PrefHub - $username</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #2c5f2d, #97b885);
            color: white;
        }
        .container { text-align: center; }
        h1 { margin-bottom: 1rem; }
        p { font-size: 1.2rem; }
    </style>
</head>
<body>
    <div class="container">
        <h1>PrefHub React</h1>
        <p id="status">Launching for $username...</p>
    </div>
    <script>
        setTimeout(() => {
            // Open React app directly - localStorage will be set on that domain
            window.location.href = '$REACT_URL?autoToken=$token&autoUsername=$username&autoGameId=$GAME_ID';
        }, 500);
    </script>
</body>
</html>
EOFLAUNCHER

    echo "Starting Player $player_num ($username)..."
    echo "  - User data: $user_dir"
    echo "  - Remote debugging port: $port"

    # Start Chrome with launcher that will redirect to React app with credentials
    "$CHROME_BIN" \
        --user-data-dir="$user_dir" \
        --remote-debugging-port=$port \
        --remote-allow-origins=* \
        --window-position=$((200 + player_num * 50)),$((100 + player_num * 50)) \
        --window-size=800,900 \
        --new-window \
        --no-default-browser-check \
        --no-first-run \
        --disable-sync \
        --disable-features=Translate,TranslateUI,PasswordManager \
        --disable-save-password-bubble \
        --disable-translate \
        --lang=en \
        "file://$launcher_file" &

    local pid=$!
    echo "  - PID: $pid"

    echo ""

    # Store PID for cleanup
    echo $pid >> "$TEMP_DIR/pids.txt"
}

# Start Chrome instances
for i in 1 2 3; do
    start_chrome_authenticated $i
    sleep 1
done

echo "=========================================="
echo "✓ React frontend test setup complete!"
echo "=========================================="
echo ""
echo "Game details:"
echo "  Game ID: $GAME_ID"
echo "  Rules: $RULES"
echo "  Players:"
for i in "${!PLAYERS[@]}"; do
    echo "    - ${PLAYERS[$i]} (password: $PASSWORD)"
done
echo ""
echo "Chrome instances are now running with authenticated React sessions!"
echo ""
echo "To stop all clients, run:"
echo "  kill \$(cat $TEMP_DIR/pids.txt)"
echo ""

# Save cleanup script
cat > "$TEMP_DIR/stop-clients.sh" << 'EOFSTOP'
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/pids.txt" ]; then
    echo "Stopping test clients..."
    while read pid; do
        if kill -0 $pid 2>/dev/null; then
            echo "Stopping process $pid"
            kill $pid
        fi
    done < "$SCRIPT_DIR/pids.txt"
    echo "All clients stopped."
else
    echo "No PID file found."
fi
EOFSTOP

chmod +x "$TEMP_DIR/stop-clients.sh"
echo "Stop script created: $TEMP_DIR/stop-clients.sh"
echo ""

# Wait for user interrupt
trap "echo ''; echo 'Stopping clients...'; $TEMP_DIR/stop-clients.sh; exit 0" INT TERM

echo "Press Ctrl+C to stop all clients..."
wait
