rm -f *.jar
cp ../jmx-agent/target/*.jar .
cp ../jmx-exporter/target/*.jar .
podman build . -t registry.sunrise.com:5000/jmx/jmx-exporter:$1
podman push registry.sunrise.com:5000/jmx/jmx-exporter:$1
