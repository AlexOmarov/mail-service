FROM quay.io/keycloak/keycloak:25.0

ARG DB_NAME
ARG DB_HOST
ARG DB_USER
ARG DB_PASSWORD
ARG DB_DRIVER

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres

RUN /opt/keycloak/bin/kc.sh build

RUN /opt/keycloak/bin/kc.sh start --db ${DB_DRIVER} --db-url jdbc:postgresql://${DB_HOST}/${DB_NAME} --db-username ${DB_USER} --db-password ${DB_PASSWORD}