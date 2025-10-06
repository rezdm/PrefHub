#!/bin/bash
# Start PrefHub server in normal mode

# Source common functions
source "$(dirname "$0")/00-start-server-common.sh"

# Build and setup
build_project
setup_classpath
copy_rules

# Start server in normal mode
start_server 8090 ./game-data
