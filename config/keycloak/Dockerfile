FROM quay.io/keycloak/keycloak:25.0

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres

COPY certificate.pem /opt/keycloak/certificate.pem
COPY private_key.pem /opt/keycloak/private_key.pem

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/bin/bash", "-c"]
CMD ["/opt/keycloak/bin/kc.sh start \
    --db ${KC_DB} \
    --db-url jdbc:postgresql://${DB_HOST}/${DB_NAME} \
    --db-username ${DB_USER} \
    --db-password ${DB_PASSWORD} \
    --https-certificate-file=/opt/keycloak/certificate.pem \
    --https-certificate-key-file=/opt/keycloak/private_key.pem \
    --hostname-strict=false"]