#!/bin/sh

# Start the undertow web server
# Usage: start_server.sh <password>

#  -Dserver.truststore=/etc/letsencrypt/live/armin-kuepfer.com/armin-kuepfer.jks \
#  -Dserver.truststore.password=$1 \


java \
  -Dserver.keystore=/etc/letsencrypt/live/armin-kuepfer.com/armin-kuepfer.jks \
  -Dserver.keystore.password=$1 \
  -Dserver.truststore=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.181-3.b13.el7_5.x86_64/jre/lib/security/cacerts \
  -Dserver.truststore.password=changeit\
  -Dkey.password=$1 \
  -cp DomainServer-1.0-SNAPSHOT.jar:./dependency/* \
  org.kuepfer.DomainServer
