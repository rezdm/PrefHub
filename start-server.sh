#!/bin/bash
cd "$(dirname "$0")"
export MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"
mvn install -N
mvn clean package -DskipTests -pl prefhub-core,prefhub-server -am
mvn exec:java -pl prefhub-server -Dexec.mainClass=com.prefhub.server.ServerMain -Dexec.args="8090 ./game-data"

