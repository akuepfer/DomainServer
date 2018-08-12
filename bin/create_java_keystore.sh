#!/bin/sh

# Create from the Let's encrypt certificate a Java keystore.
# Usage: create_java_keystore.sh <password>

openssl pkcs12 -export -in /etc/letsencrypt/live/armin-kuepfer.com/fullchain.pem -inkey /etc/letsencrypt/live/armin-kuepfer.com/privkey.pem -out /etc/letsencrypt/live/armin-kuepfer.com/armin-kuepfer.p12 -password pass:$1
keytool -importkeystore -srckeystore /etc/letsencrypt/live/armin-kuepfer.com/armin-kuepfer.p12 -srcstoretype pkcs12 -srcstorepass $1 -destkeystore /etc/letsencrypt/live/armin-kuepfer.com/armin-kuepfer.jks -deststoretype jks -deststorepass $1

