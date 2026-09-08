#!/bin/bash
set -e

echo "Creating per-service databases for smart-traffic."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE smart_traffic_auth;
    CREATE DATABASE smart_traffic_traffic;
EOSQL
echo "Databases created."
