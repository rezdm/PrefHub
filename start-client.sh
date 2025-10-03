#!/bin/bash
cd "$(dirname "$0")"
cd prefhub-client
mvn exec:java -Dexec.mainClass="com.prefhub.client.ClientMain" -Dexec.args="http://localhost:8090"
