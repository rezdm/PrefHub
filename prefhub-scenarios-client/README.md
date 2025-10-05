# PrefHub Scenarios Client

Automated test client for running PrefHub game scenarios.

## Setup

1. Install Python dependencies:
```bash
pip install -r requirements.txt
```

## Usage

Run a test scenario by name:

```bash
python scenario_runner.py <test_name>
```

Example:
```bash
python scenario_runner.py test0
```

## How It Works

1. **Reads test scenarios** from `prefhub-server/src/main/resources/test-scenarios.json`
2. **Creates 3 clients**: playerWest, playerEast, playerSouth
3. **Connects to server** at http://localhost:8090
4. **Executes moves** according to the scenario definition
5. **Validates** that the game progresses correctly

## Test Scenario Format

Each test scenario includes:
- **deck**: Initial card distribution for each player
- **scenarios**: List of move sequences to execute
  - **name**: Scenario identifier
  - **description**: What the scenario tests
  - **moves**: Sequence of actions (bid, takePrikup, playCard)

## Move Types

### Bid
```json
{
  "action": "bid",
  "player": "playerSouth",
  "contract": "SIX_HEARTS"
}
```

### Take Prikup
```json
{
  "action": "takePrikup",
  "player": "playerSouth",
  "discard": ["SEVEN_DIAMONDS", "EIGHT_DIAMONDS"]
}
```

### Play Card
```json
{
  "action": "playCard",
  "player": "playerSouth",
  "card": "ACE_HEARTS"
}
```

## Exit Codes

- `0`: Test passed
- `1`: Test failed or error occurred
