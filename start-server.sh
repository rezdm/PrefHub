#!/bin/bash
cd "$(dirname "$0")"
export MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"
mvn clean compile -DskipTests -pl prefhub-core,prefhub-server -am
cd prefhub-server
mvn exec:java -Dexec.mainClass=com.prefhub.server.ServerMain -Dexec.args="8090 ./game-data"

