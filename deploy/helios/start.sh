#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ -f app.pid ] && kill -0 "$(cat app.pid)" 2>/dev/null; then
    echo 'Приложение уже запущено.'
    exit 0
fi
umask 077
export JAVA_HOME=/usr/local/openjdk17
unset _JAVA_OPTIONS
export JAVA_OPTS='-Xms64m -Xmx384m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2 -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true'
export DB_USER="$(id -un)"
export DB_URL="jdbc:postgresql://pg:5432/studs?currentSchema=$DB_USER"
DB_PASSWORD=$(perl -e '
  open my $f, "<", "$ENV{HOME}/.pgpass" or die "Cannot read .pgpass\n";
  while (<$f>) {
    next if /^#/; chomp;
    my @v = split /(?<!\\):/, $_, 5;
    next unless @v == 5;
    s/\\([:\\])/$1/g for @v;
    next unless ($v[0] eq "pg" || $v[0] eq "*") && ($v[1] eq "5432" || $v[1] eq "*") && ($v[2] eq "studs" || $v[2] eq "*") && ($v[3] eq $ENV{DB_USER} || $v[3] eq "*");
    print $v[4]; exit 0;
  }
  die "No matching PostgreSQL credentials in .pgpass\n";
')
export DB_PASSWORD
nohup ./wildfly-35.0.1.Final/bin/standalone.sh -b 127.0.0.1 -bmanagement 127.0.0.1 -Djboss.socket.binding.port-offset=32866 > console.log 2>&1 < /dev/null &
echo $! > app.pid
echo 'Запуск: http://127.0.0.1:40946/movie-lab/ на Helios. Лог: ~/movie-lab/console.log'
