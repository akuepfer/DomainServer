#!/bin/sh

# Start the undertow web server
# Usage: start_server.sh <password>

PROJ_DIR=/home/armin/ws/DomainServer

java \
  -Dserver.keystore=/home/armin/ws/DomainServer/bin/keystore.jks \
  -Dserver.keystore.password=EneLoopBlue \
  -Dserver.truststore=/usr/lib/jvm/java-11-openjdk-amd64/lib/security/cacerts \
  -Dserver.truststore.password=changeit\
  -Dkey.password=EneLoopBlue \
  -cp $PROJ_DIR/target/DomainServer-1.0-SNAPSHOT.jar:$PROJ_DIR/target/dependency/* \
  org.kuepfer.DomainServer
