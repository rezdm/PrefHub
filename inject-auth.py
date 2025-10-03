#!/usr/bin/env python3
import json
import requests
import websocket
import sys
import time

def inject_auth(port, token, username, game_id, url):
    """Inject auth token into Chrome via DevTools Protocol"""
    try:
        # Get WebSocket URL
        response = requests.get(f'http://localhost:{port}/json')
        tabs = response.json()
        if not tabs:
            print(f"No tabs found on port {port}")
            return False

        ws_url = tabs[0]['webSocketDebuggerUrl']

        # Connect to WebSocket
        ws = websocket.create_connection(ws_url)

        # Enable Runtime and Page domains
        ws.send(json.dumps({"id": 1, "method": "Runtime.enable"}))
        ws.recv()

        ws.send(json.dumps({"id": 2, "method": "Page.enable"}))
        ws.recv()

        # Inject localStorage values
        script = f"""
        localStorage.setItem('authToken', '{token}');
        localStorage.setItem('username', '{username}');
        localStorage.setItem('currentGameId', '{game_id}');
        'injected';
        """

        ws.send(json.dumps({
            "id": 3,
            "method": "Runtime.evaluate",
            "params": {"expression": script}
        }))
        ws.recv()

        # Navigate to the page to trigger the auth
        ws.send(json.dumps({
            "id": 4,
            "method": "Page.navigate",
            "params": {"url": url}
        }))
        ws.recv()

        ws.close()
        print(f"✓ Auth injected on port {port}")
        return True

    except Exception as e:
        print(f"✗ Failed to inject auth on port {port}: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) != 6:
        print("Usage: inject-auth.py <port> <token> <username> <game_id> <url>")
        sys.exit(1)

    port = sys.argv[1]
    token = sys.argv[2]
    username = sys.argv[3]
    game_id = sys.argv[4]
    url = sys.argv[5]

    # Wait a bit for Chrome to fully start
    time.sleep(1)

    inject_auth(port, token, username, game_id, url)
