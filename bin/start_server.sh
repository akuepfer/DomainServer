#!/bin/sh

# Start the undertow web server
# Usage: start_server.sh <password>

#  -Dserver.truststore=/etc/letsencrypt/live/armin-kuepfer.com/armin-kuepfer.jks \
#  -Dserver.truststore.password=$1 \

JAVA_HOME=/usr/local/jdk-10.0.2
TARGET=/home/armin/ws/DomainServer/target
LOGDIR=/home/armin/ws/DomainServer/log


find /home/armin/ws/MoveMountains/app -name '*.undertow.encoding.gzip' -exec rm {} \;


cd /home/armin/ws/DomainServer/root

nohup java \
  -Dserver.keystore=/etc/letsencrypt/live/armin-kuepfer.com/armin-kuepfer.jks \
  -Dserver.keystore.password=$1 \
  -Dserver.truststore=$JAVA_HOME/lib/security/cacerts \
  -Dserver.truststore.password=changeit\
  -Dkey.password=$1 \
  -cp $TARGET/DomainServer-1.0-SNAPSHOT.jar:$TARGET/dependency/* \
  org.kuepfer.DomainServer 2>&1 > $LOGDIR/domain-server.log &

