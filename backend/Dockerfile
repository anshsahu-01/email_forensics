# syntax=docker/dockerfile:1.7

# =========================
# 1. Build Spring Boot app
# =========================
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests


# =========================
# 2. Download MaxMind DBs
# =========================
FROM ghcr.io/maxmind/geoipupdate:8.0.0 AS maxmind

RUN --mount=type=secret,id=maxmind_account_id,dst=/run/secrets/maxmind_account_id \
    --mount=type=secret,id=maxmind_license_key,dst=/run/secrets/maxmind_license_key \
    GEOIPUPDATE_ACCOUNT_ID_FILE=/run/secrets/maxmind_account_id \
    GEOIPUPDATE_LICENSE_KEY_FILE=/run/secrets/maxmind_license_key \
    GEOIPUPDATE_EDITION_IDS="GeoLite2-City GeoLite2-ASN" \
    GEOIPUPDATE_DB_DIR=/usr/share/GeoIP \
    geoipupdate


# =========================
# 3. Runtime image
# =========================
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN mkdir -p /app/data

# Copy downloaded MaxMind databases
COPY --from=maxmind /usr/share/GeoIP/GeoLite2-City.mmdb /app/data/GeoLite2-City.mmdb
COPY --from=maxmind /usr/share/GeoIP/GeoLite2-ASN.mmdb /app/data/GeoLite2-ASN.mmdb

# MaxMind paths used by Spring Boot
ENV MAXMIND_DB_PATH=/app/data/GeoLite2-City.mmdb
ENV MAXMIND_ASN_DB_PATH=/app/data/GeoLite2-ASN.mmdb

# Copy Spring Boot JAR
COPY --from=build /app/target/emailForensic-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]