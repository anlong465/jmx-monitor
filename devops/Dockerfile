FROM docker.io/library/openjdk:11

RUN mkdir /app
COPY *.jar  /app

ENV JVMOPT=
ENV JVM_MASTER=HotSpot

ENTRYPOINT ["sh", "-c", "java $JVMOPT -jar /app/jmx-exporter-1.0.jar"]

