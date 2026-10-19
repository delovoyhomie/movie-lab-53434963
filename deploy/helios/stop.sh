#!/bin/sh
set -eu
cd "$(dirname "$0")"
export JAVA_HOME=/usr/local/openjdk17
unset _JAVA_OPTIONS
export JAVA_OPTS='-Xms32m -Xmx128m -XX:MaxMetaspaceSize=128m -XX:ActiveProcessorCount=2'
./wildfly-35.0.1.Final/bin/jboss-cli.sh --connect --controller=127.0.0.1:42856 --command=':shutdown'
rm -f app.pid
