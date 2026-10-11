# syntax=docker/dockerfile:1

# ============ Stage 1: build the jar ============
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ============ Stage 2: app + MariaDB in ONE image ============
FROM eclipse-temurin:21-jre
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends mariadb-server \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --no-create-home spring

WORKDIR /app
COPY --from=build /app/target/expense-tracker-1.0.0.jar app.jar

# App -> local MariaDB (override DB_* on Render if you want an external DB)
ENV DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=finishers \
    DB_USERNAME=app \
    THYMELEAF_CACHE=true

# Startup script: starts MariaDB, prepares the database, then starts the app
COPY <<'SCRIPT' /app/start.sh
#!/bin/bash
set -e

: "${DB_PASSWORD:?Set the DB_PASSWORD environment variable}"
: "${JWT_SECRET:?Set the JWT_SECRET environment variable (32+ chars)}"

DATADIR=/var/lib/mysql/data
mkdir -p "$DATADIR" /run/mysqld
chown -R mysql:mysql /var/lib/mysql /run/mysqld

# First start (or empty disk): initialise the data directory
if [ ! -d "$DATADIR/mysql" ]; then
  echo ">> Initialising MariaDB data directory"
  mariadb-install-db --user=mysql --datadir="$DATADIR" --skip-test-db > /dev/null
fi

echo ">> Starting MariaDB"
mariadbd --user=mysql --datadir="$DATADIR" \
  --bind-address=127.0.0.1 --port="$DB_PORT" \
  --innodb-buffer-pool-size=64M --performance-schema=OFF &
DB_PID=$!

for i in $(seq 1 60); do
  mariadb-admin ping --silent 2>/dev/null && break
  sleep 1
done
mariadb-admin ping --silent || { echo "MariaDB failed to start"; exit 1; }

# Create database + app user (idempotent; password re-synced on every start)
PW=${DB_PASSWORD//\'/\'\'}
mariadb <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${PW}';
CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'127.0.0.1' IDENTIFIED BY '${PW}';
ALTER USER '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${PW}';
ALTER USER '${DB_USERNAME}'@'127.0.0.1' IDENTIFIED BY '${PW}';
GRANT ALL ON \`${DB_NAME}\`.* TO '${DB_USERNAME}'@'localhost';
GRANT ALL ON \`${DB_NAME}\`.* TO '${DB_USERNAME}'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

echo ">> Starting Expense Tracker on port ${PORT:-8080}"
setpriv --reuid=spring --regid=spring --init-groups \
  java -XX:MaxRAMPercentage=60 -Dserver.port="${PORT:-8080}" -jar /app/app.jar &
APP_PID=$!

# Graceful shutdown: stop app first, then the database
trap 'kill -TERM $APP_PID 2>/dev/null; wait $APP_PID 2>/dev/null; mariadb-admin shutdown 2>/dev/null; exit 0' TERM INT

wait $APP_PID
mariadb-admin shutdown 2>/dev/null || true
SCRIPT

RUN chmod +x /app/start.sh

# Render sets PORT at runtime; 8080 is the local default
EXPOSE 8080
CMD ["/app/start.sh"]
