#!/usr/bin/env python3
"""
PrefHub Scenario Test Runner
Runs automated test scenarios against the PrefHub server
"""

import sys
import json
import requests
import time
from typing import Dict, List, Optional
from pathlib import Path


class PrefHubClient:
    """Client for interacting with PrefHub server"""

    def __init__(self, base_url: str = "http://localhost:8090"):
        self.base_url = base_url
        self.session_token: Optional[str] = None
        self.username: Optional[str] = None

    @staticmethod
    def parse_card(card_str: str) -> Dict:
        """Convert card string like 'SEVEN_DIAMONDS' to Card object"""
        parts = card_str.split('_')
        if len(parts) != 2:
            raise ValueError(f"Invalid card format: {card_str}")
        return {"suit": parts[1], "rank": parts[0]}

    def login(self, username: str, password: str) -> str:
        """Login with existing user and return session token"""
        response = requests.post(
            f"{self.base_url}/api/auth/login",
            json={"username": username, "password": password}
        )
        response.raise_for_status()
        data = response.json()
        self.session_token = data["token"]
        self.username = username
        return self.session_token

    def create_game(self, game_id: str, rule_id: str = "sochinka") -> Dict:
        """Create a new game"""
        headers = {"Authorization": f"Bearer {self.session_token}"}
        response = requests.post(
            f"{self.base_url}/api/games/create",
            json={"gameId": game_id, "ruleId": rule_id},
            headers=headers
        )
        response.raise_for_status()
        return response.json()

    def join_game(self, game_id: str) -> Dict:
        """Join an existing game"""
        headers = {"Authorization": f"Bearer {self.session_token}"}
        response = requests.post(
            f"{self.base_url}/api/games/join",
            json={"gameId": game_id},
            headers=headers
        )
        response.raise_for_status()
        return response.json()

    def get_game_state(self, game_id: str) -> Dict:
        """Get current game state"""
        headers = {"Authorization": f"Bearer {self.session_token}"}
        response = requests.get(
            f"{self.base_url}/api/games/state?gameId={game_id}",
            headers=headers
        )
        response.raise_for_status()
        return response.json()

    def place_bid(self, game_id: str, contract: str) -> Dict:
        """Place a bid"""
        headers = {"Authorization": f"Bearer {self.session_token}"}
        response = requests.post(
            f"{self.base_url}/api/games/bid",
            json={"gameId": game_id, "contract": contract},
            headers=headers
        )
        response.raise_for_status()
        return response.json()

    def take_prikup(self, game_id: str, discard: List[str]) -> Dict:
        """Take prikup and discard cards"""
        headers = {"Authorization": f"Bearer {self.session_token}"}
        # Convert card strings to Card objects
        card_objects = [self.parse_card(card) for card in discard]
        response = requests.post(
            f"{self.base_url}/api/games/exchange",
            json={"gameId": game_id, "cards": card_objects},
            headers=headers
        )
        response.raise_for_status()
        return response.json()

    def play_card(self, game_id: str, card: str) -> Dict:
        """Play a card"""
        headers = {"Authorization": f"Bearer {self.session_token}"}
        # Convert card string to Card object
        card_object = self.parse_card(card)
        response = requests.post(
            f"{self.base_url}/api/games/play",
            json={"gameId": game_id, "card": card_object},
            headers=headers
        )
        response.raise_for_status()
        return response.json()


class ScenarioRunner:
    """Runs test scenarios from JSON configuration"""

    def __init__(self, scenarios_file: str):
        self.scenarios_file = Path(scenarios_file)
        self.scenarios = self._load_scenarios()
        self.clients: Dict[str, PrefHubClient] = {}

    def _load_scenarios(self) -> Dict:
        """Load scenarios from JSON file"""
        with open(self.scenarios_file, 'r') as f:
            return json.load(f)

    def load_session_tokens(self) -> Dict[str, str]:
        """Load pre-created session tokens from the test storage directory"""
        # The server writes tokens to a test-tokens.json file in the storage directory
        # We need to find the most recent test storage directory
        import glob
        import os

        # Look in the project root directory (parent of prefhub-scenarios-client)
        project_root = Path(__file__).parent.parent
        search_pattern = str(project_root / "game-data-scenarios" / "*")
        test_dirs = glob.glob(search_pattern)

        if not test_dirs:
            raise Exception(f"No test storage directory found in {project_root}/game-data-scenarios. Is the server running in scenario mode?")

        # Use the most recent directory
        test_dir = max(test_dirs, key=lambda p: Path(p).stat().st_mtime)
        tokens_file = Path(test_dir) / "test-tokens.json"

        if not tokens_file.exists():
            raise Exception(f"Token file not found: {tokens_file}")

        print(f"  Loading tokens from: {tokens_file}")
        with open(tokens_file, 'r') as f:
            return json.load(f)

    def setup_clients(self, test_name: str) -> None:
        """Setup three clients for the test"""
        print(f"Setting up clients for test: {test_name}")

        # Load the session tokens that were pre-created by the server
        print("Loading session tokens from server storage...")
        tokens = self.load_session_tokens()

        # Create clients with the pre-existing tokens
        for player in ["playerWest", "playerEast", "playerSouth"]:
            client = PrefHubClient()
            client.session_token = tokens[player]
            client.username = player
            self.clients[player] = client
            print(f"  Loaded session for {player}: {tokens[player][:20]}...")

    def run_test(self, test_name: str) -> bool:
        """Run a specific test scenario"""
        if test_name not in self.scenarios["tests"]:
            print(f"Error: Test '{test_name}' not found in scenarios")
            return False

        test_data = self.scenarios["tests"][test_name]
        game_id = test_data["game"]

        print(f"\n{'='*60}")
        print(f"Running test: {test_name}")
        print(f"Description: {test_data['description']}")
        print(f"Game ID: {game_id}")
        print(f"{'='*60}\n")

        # Setup clients (loads pre-created session tokens)
        self.setup_clients(test_name)

        # In scenario mode, the server already created the game, added players, and set up the deck
        # We just need to start executing the scenario moves
        print(f"\nGame '{game_id}' is already set up by the server")
        print(f"Players are already in the game with custom deck")

        # Check if scenarios exist
        if "scenarios" not in test_data or not test_data["scenarios"]:
            print("\nWarning: No scenarios defined for this test")
            return True

        # Run each scenario
        for scenario in test_data["scenarios"]:
            scenario_name = scenario["name"]
            print(f"\n--- Running scenario: {scenario_name} ---")
            print(f"    {scenario['description']}")

            success = self._run_scenario(game_id, scenario)
            if not success:
                print(f"✗ Scenario {scenario_name} FAILED")
                return False

            print(f"✓ Scenario {scenario_name} PASSED")

        return True

    def _run_scenario(self, game_id: str, scenario: Dict) -> bool:
        """Execute a single scenario"""
        moves = scenario.get("moves", [])

        for i, move in enumerate(moves):
            action = move["action"]
            player = move["player"]
            client = self.clients[player]

            print(f"\n  Move {i+1}: {player} - {action}")

            try:
                if action == "bid":
                    contract = move["contract"]
                    print(f"    Bidding: {contract}")
                    result = client.place_bid(game_id, contract)

                elif action == "takePrikup":
                    discard = move["discard"]
                    print(f"    Taking prikup, discarding: {discard}")
                    result = client.take_prikup(game_id, discard)

                elif action == "playCard":
                    card = move["card"]
                    print(f"    Playing card: {card}")
                    result = client.play_card(game_id, card)

                else:
                    print(f"    Unknown action: {action}")
                    return False

                # Brief pause between moves
                time.sleep(0.1)

            except requests.exceptions.HTTPError as e:
                print(f"    ✗ Error: {e.response.status_code} - {e.response.text}")
                return False
            except Exception as e:
                print(f"    ✗ Error: {e}")
                return False

        # Verify final game state
        print("\n  Verifying final game state...")
        try:
            final_state = self.clients["playerSouth"].get_game_state(game_id)
            print(f"    Game phase: {final_state.get('phase', 'unknown')}")

            # Check if game completed successfully
            if final_state.get("phase") == "GAME_OVER":
                print("    ✓ Game completed successfully")

        except Exception as e:
            print(f"    Warning: Could not verify final state: {e}")

        return True


def main():
    if len(sys.argv) < 2:
        print("Usage: python scenario_runner.py <test_name>")
        print("\nExample: python scenario_runner.py test0")
        sys.exit(1)

    test_name = sys.argv[1]

    # Find scenarios file
    scenarios_file = Path(__file__).parent.parent / "prefhub-server" / "src" / "main" / "resources" / "test-scenarios.json"

    if not scenarios_file.exists():
        print(f"Error: Scenarios file not found: {scenarios_file}")
        sys.exit(1)

    # Run the test
    runner = ScenarioRunner(str(scenarios_file))
    success = runner.run_test(test_name)

    if success:
        print(f"\n{'='*60}")
        print(f"✓ Test '{test_name}' PASSED")
        print(f"{'='*60}\n")
        sys.exit(0)
    else:
        print(f"\n{'='*60}")
        print(f"✗ Test '{test_name}' FAILED")
        print(f"{'='*60}\n")
        sys.exit(1)


if __name__ == "__main__":
    main()
