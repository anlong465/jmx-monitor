export JMX_OPTS="-Xms180m -Xmx360m"

# export JMX_OPTS="$JMX_OPTS -Dlogging.level.org.sunrise.jmx.server.controller=DEBUG"

>nohup.out
nohup /opt/java-11/bin/java $JMX_OPTS -jar ./jmx-exporter-1.0.jar --server.port=5555 &

