#!/bin/bash
# Start PrefHub React frontend

# Load nvm and use Node.js 22
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
nvm use 22 > /dev/null 2>&1

cd "$(dirname "$0")/prefhub-frontend"

echo "Starting PrefHub frontend on http://localhost:3000"
echo "API requests will be proxied to http://localhost:8090"
echo ""

npm run dev
