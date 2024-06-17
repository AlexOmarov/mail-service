CREATE EXTENSION "uuid-ossp";
CREATE EXTENSION "pg_stat_statements";

CREATE USER keycloak WITH ENCRYPTED PASSWORD 'keycloak';
CREATE DATABASE keycloak OWNER keycloak;