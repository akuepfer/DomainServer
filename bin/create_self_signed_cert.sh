#!/usr/bin/env bash


keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass EneLoopBlue -validity 360 -keysize 2048
