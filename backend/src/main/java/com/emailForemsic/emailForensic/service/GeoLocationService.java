package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.GeoLocationResult;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolves approximate geolocation for a public IP address using a local MaxMind
 * GeoIP2 (GeoLite2-City) database file.
 *
 * <p>The {@code DatabaseReader} is opened once at startup and reused for all lookups
 * (thread-safe per MaxMind documentation). If the database path is not configured or
 * the file cannot be opened, the service starts without a reader and returns unknown
 * results for every lookup. All lookup failures are non-fatal.
 *
 * <p>Database setup:
 * <ol>
 *   <li>Register for a free MaxMind account and download {@code GeoLite2-City.mmdb}.</li>
 *   <li>Set {@code MAXMIND_DB_PATH=/absolute/path/to/GeoLite2-City.mmdb} in your
 *       local {@code backend/.env} file.</li>
 *   <li>The file must NOT be committed to version control (see .gitignore).</li>
 * </ol>
 */
@Service
public class GeoLocationService {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationService.class);

    @Value("${maxmind.database-path:}")
    private String databasePath;

    private DatabaseReader databaseReader;

    @PostConstruct
    private void init() {
        if (databasePath == null || databasePath.isBlank()) {
            log.info("MaxMind database path not configured (maxmind.database-path is empty) — "
                    + "IP geolocation will return unknown for all lookups.");
            return;
        }

        File dbFile = new File(databasePath);
        if (!dbFile.exists() || !dbFile.isFile()) {
            log.error("MaxMind database file not found at '{}' — "
                    + "IP geolocation will return unknown for all lookups.", databasePath);
            return;
        }

        try {
            databaseReader = new DatabaseReader.Builder(dbFile).build();
            log.info("MaxMind GeoIP2 database loaded from '{}'.", databasePath);
        } catch (IOException e) {
            log.error("Failed to open MaxMind database at '{}': {} — "
                    + "IP geolocation will return unknown for all lookups.",
                    databasePath, e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        if (databaseReader != null) {
            try {
                databaseReader.close();
                log.info("MaxMind GeoIP2 database reader closed.");
            } catch (IOException e) {
                log.warn("Error closing MaxMind database reader: {}", e.getMessage());
            }
        }
    }

    /**
     * Looks up approximate geolocation for the given IP address.
     *
     * <p>Returns an empty (all-null) {@link GeoLocationResult} when:
     * <ul>
     *   <li>the IP is null, blank, private, or loopback</li>
     *   <li>the MaxMind database is not configured or could not be opened</li>
     *   <li>the database has no record for the address</li>
     *   <li>any MaxMind or I/O exception occurs</li>
     * </ul>
     *
     * <p>This method never throws. Failures are logged and an empty result is returned
     * so that callers can continue email analysis uninterrupted.
     *
     * @param ipAddress a public IP address string (IPv4 or IPv6); may be null
     * @return a non-null {@link GeoLocationResult}; fields are null when unresolvable
     */
    public GeoLocationResult lookup(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return emptyResult();
        }

        if (databaseReader == null) {
            return emptyResult();
        }

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(ipAddress.trim());
        } catch (UnknownHostException e) {
            log.debug("GeoLocation: could not parse IP address '{}': {}", ipAddress, e.getMessage());
            return emptyResult();
        }

        // Defense-in-depth: skip private/loopback/link-local addresses even though
        // EmailParserService.isPublicIp() should have already filtered them out.
        if (inetAddress.isLoopbackAddress()
                || inetAddress.isSiteLocalAddress()
                || inetAddress.isLinkLocalAddress()
                || inetAddress.isAnyLocalAddress()) {
            log.debug("GeoLocation: skipping private/local address '{}'.", ipAddress);
            return emptyResult();
        }

        try {
            CityResponse city = databaseReader.city(inetAddress);
            return GeoLocationResult.builder()
                    .country(nullIfBlank(city.getCountry().getName()))
                    .city(nullIfBlank(city.getCity().getName()))
                    .latitude(city.getLocation().getLatitude())
                    .longitude(city.getLocation().getLongitude())
                    .timezone(nullIfBlank(city.getLocation().getTimeZone()))
                    .build();

        } catch (AddressNotFoundException e) {
            // Normal for IPs without a MaxMind record (e.g. some IPv6, reserved ranges).
            log.debug("GeoLocation: no record found for IP '{}' in MaxMind database.", ipAddress);
            return emptyResult();
        } catch (GeoIp2Exception e) {
            log.warn("GeoLocation: MaxMind GeoIp2Exception for IP '{}': {}", ipAddress, e.getMessage());
            return emptyResult();
        } catch (IOException e) {
            log.warn("GeoLocation: IOException during lookup for IP '{}': {}", ipAddress, e.getMessage());
            return emptyResult();
        }
    }

    private GeoLocationResult emptyResult() {
        return GeoLocationResult.builder().build();
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
