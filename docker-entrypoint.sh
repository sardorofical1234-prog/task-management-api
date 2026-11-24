#!/bin/sh
set -eu

# Render provides PostgreSQL's native connection URI. The PostgreSQL JDBC driver
# expects the same URI with the "jdbc:" prefix. Local Compose already uses JDBC.
case "${DB_URL:-}" in
  postgresql://*)
    DB_URL="jdbc:${DB_URL}"
    export DB_URL
    ;;
esac

exec java -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -jar /app/app.jar
