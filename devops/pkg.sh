rm -rf jmx-package*

mkdir -p jmx-package/grafana

cp ../jmx-agent-assembly/target/jmx-agent-1.0.jar jmx-package
cp ../jmx-exporter/target/*.jar jmx-package

cp grafana/*.json jmx-package/grafana
cp prometheus.yml jmx-package

cp start-jmx-exporter.sh  jmx-package

zip jmx-package.zip jmx-package/*

#rm -rf jmx-package

