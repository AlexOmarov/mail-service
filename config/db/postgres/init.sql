CREATE EXTENSION "uuid-ossp";
CREATE EXTENSION "pg_stat_statements";

CREATE USER keycloack LOGIN WITH PASSWORD 'keycloack';

CREATE DATABASE keycloack OWNER keycloack;
