FROM openjdk:21-jdk-slim

RUN groupadd --system --gid 800 appuser && \
    useradd --system --uid 800 --gid 800 appuser
USER appuser
COPY .build/app/libs/app.jar /app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/var/dumps", "-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]

