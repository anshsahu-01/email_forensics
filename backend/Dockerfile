# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests


FROM eclipse-temurin:17-jre

WORKDIR /app

# MaxMind database directory
RUN mkdir -p /app/data

# Install tools needed to download MaxMind databases
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Download GeoLite2 City + ASN databases using Render build secrets
RUN --mount=type=secret,id=maxmind_account_id \
    --mount=type=secret,id=maxmind_license_key \
    ACCOUNT_ID="$(cat /run/secrets/maxmind_account_id)" \
    LICENSE_KEY="$(cat /run/secrets/maxmind_license_key)" \
    curl -fL --retry 3 \
    -u "${ACCOUNT_ID}:${LICENSE_KEY}" \
    "https://download.maxmind.com/geoip/databases/GeoLite2-City/download?suffix=tar.gz" \
    -o /tmp/GeoLite2-City.tar.gz \
    && tar -xzf /tmp/GeoLite2-City.tar.gz -C /tmp \
    && find /tmp -name "GeoLite2-City.mmdb" -exec cp {} /app/data/GeoLite2-City.mmdb \; \
    && rm -rf /tmp/GeoLite2-City*


RUN --mount=type=secret,id=maxmind_account_id \
    --mount=type=secret,id=maxmind_license_key \
    ACCOUNT_ID="$(cat /run/secrets/maxmind_account_id)" \
    LICENSE_KEY="$(cat /run/secrets/maxmind_license_key)" \
    curl -fL --retry 3 \
    -u "${ACCOUNT_ID}:${LICENSE_KEY}" \
    "https://download.maxmind.com/geoip/databases/GeoLite2-ASN/download?suffix=tar.gz" \
    -o /tmp/GeoLite2-ASN.tar.gz \
    && tar -xzf /tmp/GeoLite2-ASN.tar.gz -C /tmp \
    && find /tmp -name "GeoLite2-ASN.mmdb" -exec cp {} /app/data/GeoLite2-ASN.mmdb \; \
    && rm -rf /tmp/GeoLite2-ASN*


# Spring Boot configuration
ENV MAXMIND_DB_PATH=/app/data/GeoLite2-City.mmdb
ENV MAXMIND_ASN_DB_PATH=/app/data/GeoLite2-ASN.mmdb

COPY --from=build /app/target/emailForensic-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]