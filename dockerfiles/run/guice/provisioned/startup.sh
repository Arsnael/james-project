#!/bin/bash

wait-for-it.sh --host=localhost --port=9999 --strict --timeout=0 -- ./initialdata.sh &

java -classpath '/root/james-server.jar:/root/james-server-jpa-guice.lib/*' -javaagent:/root/james-server-jpa-guice.lib/openjpa-3.1.0.jar -Dlogback.configurationFile=/root/conf/logback.xml -Dworking.directory=/root/ -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n org.apache.james.JPAJamesServerMain