#!/bin/bash
# Start PrefHub React frontend

cd "$(dirname "$0")/prefhub-frontend"

echo "Starting PrefHub frontend on http://localhost:3000"
echo "API requests will be proxied to http://localhost:8090"
echo ""

npm run dev
