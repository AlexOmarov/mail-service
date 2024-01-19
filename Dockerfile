FROM openjdk:21-jdk-slim

RUN groupadd --system --gid 800 appuser && \
    useradd --system --uid 800 --gid 800 appuser

RUN  apt-get update \
  && apt-get install -y curl \
  && apt-get clean

USER appuser
COPY .build/app/libs/app.jar /app.jar
EXPOSE 8080 9010 9090 7000
ENTRYPOINT ["java", \
    "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/var/dumps", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dcom.sun.management.jmxremote", \
    "-Dcom.sun.management.jmxremote.port=9010", \
    "-Dcom.sun.management.jmxremote.rmi.port=9010", \
    "-Dcom.sun.management.jmxremote.local.only=false", \
    "-Dcom.sun.management.jmxremote.authenticate=false", \
    "-Dcom.sun.management.jmxremote.ssl=false", \
    "-Djava.rmi.server.hostname=0.0.0.0", \
    "-jar","/app.jar"]
