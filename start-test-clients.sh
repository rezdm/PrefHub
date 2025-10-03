#!/bin/bash

# Script to start 3 Chrome instances for testing PrefHub
# Each instance has its own user profile to maintain separate sessions

# Configuration
PREFHUB_URL="${PREFHUB_URL:-http://localhost:8090}"
NUM_CLIENTS="${NUM_CLIENTS:-3}"
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
    echo "Please install Chrome or set CHROME_BIN environment variable"
    exit 1
fi

echo "Using Chrome: $CHROME_BIN"
echo "PrefHub URL: $PREFHUB_URL"
echo "Starting $NUM_CLIENTS test clients..."
echo ""

# Create temp directory for user data
TEMP_DIR=$(mktemp -d -t prefhub-test-XXXXXX)
echo "User data directory: $TEMP_DIR"
echo ""

# Function to start a Chrome instance
start_chrome() {
    local player_num=$1
    local port=$((9222 + player_num))
    local user_dir="$TEMP_DIR/player$player_num"

    mkdir -p "$user_dir/Default"

    # Create preferences to disable password manager and translation
    cat > "$user_dir/Default/Preferences" << 'EOF'
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
EOF

    echo "Starting Player $player_num..."
    echo "  - User data: $user_dir"
    echo "  - Remote debugging port: $port"

    "$CHROME_BIN" \
        --user-data-dir="$user_dir" \
        --remote-debugging-port=$port \
        --window-position=$((200 + player_num * 50)),$((100 + player_num * 50)) \
        --window-size=800,900 \
        --new-window \
        --no-default-browser-check \
        --no-first-run \
        --disable-sync \
        --disable-features=Translate,TranslateUI,PasswordManager \
        --disable-save-password-bubble \
        --disable-translate \
        --lang=ru \
        "$PREFHUB_URL" &

    local pid=$!
    echo "  - PID: $pid"
    echo ""

    # Store PID for cleanup
    echo $pid >> "$TEMP_DIR/pids.txt"
}

# Start Chrome instances
for i in $(seq 1 $NUM_CLIENTS); do
    start_chrome $i
    sleep 1  # Small delay between starts
done

echo "=========================================="
echo "Test clients started successfully!"
echo "=========================================="
echo ""
echo "Player profiles:"
for i in $(seq 1 $NUM_CLIENTS); do
    echo "  Player $i: $(ls -d $TEMP_DIR/player$i)"
done
echo ""
echo "To stop all clients, run:"
echo "  kill \$(cat $TEMP_DIR/pids.txt)"
echo ""
echo "Or press Ctrl+C and run the stop script"
echo ""

# Save cleanup script
cat > "$TEMP_DIR/stop-clients.sh" << 'EOF'
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
    echo "User data preserved in: $SCRIPT_DIR"
else
    echo "No PID file found."
fi
EOF

chmod +x "$TEMP_DIR/stop-clients.sh"

echo "Stop script created: $TEMP_DIR/stop-clients.sh"
echo ""

# Wait for user interrupt
trap "echo ''; echo 'Stopping clients...'; $TEMP_DIR/stop-clients.sh; exit 0" INT TERM

echo "Press Ctrl+C to stop all clients..."
wait
