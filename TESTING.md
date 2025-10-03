# PrefHub Testing Guide

This guide explains how to test the PrefHub multiplayer card game.

## Prerequisites

- PrefHub server running
- Chrome or Chromium browser installed
- `curl` and `jq` (for automated setup script)

## Quick Start

### 1. Start the Server

```bash
./start-server.sh
```

The server will start on http://localhost:8090

### 2. Option A: Manual Testing with Browser Instances

Start 3 separate Chrome instances with isolated user profiles:

```bash
./start-test-clients.sh
```

This will:
- Launch 3 Chrome windows
- Each with its own user profile (separate cookies/localStorage)
- All pointing to http://localhost:8090
- Positioned in a cascade for easy viewing

**Manual steps in each browser:**
1. Register a new user (e.g., alice, bob, charlie)
2. Create a game in first browser (choose rules variant)
3. Join the game from other browsers
4. Play!

To stop all clients:
```bash
# The script will show the stop command, or press Ctrl+C
```

### 3. Option B: Automated Setup

Use the automated script to create users and a game:

```bash
./setup-test-game.sh
```

This will:
- Create 3 test users: alice, bob, charlie (password: password123)
- Create a game with a unique ID
- Have all 3 players join the game

Then start the browsers:
```bash
./start-test-clients.sh
```

Login each browser with:
- Browser 1: alice / password123
- Browser 2: bob / password123
- Browser 3: charlie / password123

## Test Scenarios

### Testing Different Rule Variants

Create games with different rules:

```bash
# Sochinka (southern, relaxed rules)
RULES=sochinka ./setup-test-game.sh

# Leningradka (classical Petersburg rules) - default
RULES=leningradka ./setup-test-game.sh

# Stalingradka (strict Volgograd rules)
RULES=stalingradka ./setup-test-game.sh
```

### Testing Game Flow

1. **Bidding Phase**: Each player makes bids or passes
2. **Widow Exchange**: Declarer takes widow and discards 2 cards
3. **Playing Phase**: Players play cards following suit
4. **Scoring**: Check score calculation
5. **Next Round**: Start a new round

### Testing Rules Validation

Try these scenarios to test rule validation:

1. **Minimum bid**: Try bidding lower than 6 (should fail)
2. **6 Spades mandatory whist** (Leningradka): Check if whist is enforced
3. **Half-whist** (Sochinka allows, Stalingradka forbids)
4. **Invalid bid sequence**: Try bidding lower than current highest

## Configuration

### Environment Variables

```bash
# Change PrefHub URL
PREFHUB_URL=http://localhost:9000 ./start-test-clients.sh

# Change number of clients
NUM_CLIENTS=4 ./start-test-clients.sh

# Custom game ID
GAME_ID=my-test-game ./setup-test-game.sh
```

## Troubleshooting

### Chrome not found

Set the Chrome binary path:
```bash
CHROME_BIN=/path/to/chrome ./start-test-clients.sh
```

### Port already in use

The script uses ports 9222-9224 for remote debugging. If these are in use, kill the processes:
```bash
lsof -ti:9222,9223,9224 | xargs kill -9
```

### Server not responding

Check if the server is running:
```bash
curl http://localhost:8090
```

Restart the server:
```bash
./start-server.sh
```

### Clean up test data

Remove all test user profiles:
```bash
rm -rf /tmp/prefhub-test-*
```

## Manual API Testing

### Register a user
```bash
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}'
```

### Login
```bash
TOKEN=$(curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}' | jq -r '.token')
```

### List available rules
```bash
curl http://localhost:8090/api/rules/list
```

### Create a game
```bash
curl -X POST http://localhost:8090/api/games/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"gameId":"test-game","ruleId":"sochinka"}'
```

### Get available bids
```bash
curl "http://localhost:8090/api/games/available-bids?gameId=test-game" \
  -H "Authorization: Bearer $TOKEN"
```

## Development Tips

### Watch logs

```bash
# Server logs
tail -f game-data/logs/prefhub.log

# Or use journalctl if running as service
journalctl -u prefhub -f
```

### Debug a specific client

Each Chrome instance has a remote debugging port (9222, 9223, 9224). Open in browser:
```
http://localhost:9222
```

### Inspect game state

```bash
# Get game state for a specific player
curl "http://localhost:8090/api/games/state?gameId=test-game" \
  -H "Authorization: Bearer $TOKEN" | jq
```

## CI/CD Testing

For automated testing in CI/CD pipelines, use headless mode:

```bash
# Add to start-test-clients.sh Chrome args:
--headless=new \
--disable-gpu \
--no-sandbox
```

Then run Selenium or Puppeteer tests against the instances.
