#build
FROM gradle:8-jdk21-alpine AS build
RUN mkdir -p /tmp
COPY ./ /tmp
USER root
WORKDIR /tmp
RUN gradle clean build -x test -x detekt

#pack
FROM openjdk:21-jdk-slim

RUN groupadd --system --gid 1000 appuser && \
    useradd --system --uid 1000 --gid 1000 appuser
USER appuser
COPY --from=build /tmp/.build/app/libs/app.jar /app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/var/dumps", "-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]

