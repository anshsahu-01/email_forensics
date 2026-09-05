package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.AsnResult;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
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
 * Resolves ASN / network intelligence for a public IP address using a local
 * MaxMind GeoLite2-ASN database file.
 *
 * <p>The {@code DatabaseReader} is opened once at startup and reused for all lookups
 * (thread-safe per MaxMind documentation). If the database path is not configured or
 * the file cannot be opened, the service starts without a reader and returns empty
 * results for every lookup. All lookup failures are non-fatal.
 *
 * <p>GeoLite2-ASN provides two fields per IP:
 * <ul>
 *   <li>Autonomous System Number (integer) — stored with an {@code AS} prefix, e.g. {@code "AS15169"}</li>
 *   <li>Autonomous System Organization name — e.g. {@code "GOOGLE"}</li>
 * </ul>
 *
 * <p>Database setup:
 * <ol>
 *   <li>Register for a free MaxMind account and download {@code GeoLite2-ASN.mmdb}.</li>
 *   <li>Set {@code MAXMIND_ASN_DB_PATH=/absolute/path/to/GeoLite2-ASN.mmdb} in your
 *       local {@code backend/.env} file.</li>
 *   <li>The file must NOT be committed to version control (see .gitignore).</li>
 * </ol>
 */
@Service
public class AsnService {

    private static final Logger log = LoggerFactory.getLogger(AsnService.class);

    @Value("${maxmind.asn-database-path:}")
    private String databasePath;

    private DatabaseReader databaseReader;

    @PostConstruct
    private void init() {
        if (databasePath == null || databasePath.isBlank()) {
            log.info("MaxMind ASN database path not configured (maxmind.asn-database-path is empty) — "
                    + "ASN lookup will return unknown for all lookups.");
            return;
        }

        File dbFile = new File(databasePath);
        if (!dbFile.exists() || !dbFile.isFile()) {
            log.error("MaxMind ASN database file not found at '{}' — "
                    + "ASN lookup will return unknown for all lookups.", databasePath);
            return;
        }

        try {
            databaseReader = new DatabaseReader.Builder(dbFile).build();
            log.info("MaxMind GeoLite2-ASN database loaded from '{}'.", databasePath);
        } catch (IOException e) {
            log.error("Failed to open MaxMind ASN database at '{}': {} — "
                    + "ASN lookup will return unknown for all lookups.",
                    databasePath, e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        if (databaseReader != null) {
            try {
                databaseReader.close();
                log.info("MaxMind GeoLite2-ASN database reader closed.");
            } catch (IOException e) {
                log.warn("Error closing MaxMind ASN database reader: {}", e.getMessage());
            }
        }
    }

    /**
     * Looks up ASN / network intelligence for the given IP address.
     *
     * <p>Returns an empty (all-null) {@link AsnResult} when:
     * <ul>
     *   <li>the IP is null, blank, private, or loopback</li>
     *   <li>the MaxMind ASN database is not configured or could not be opened</li>
     *   <li>the database has no record for the address</li>
     *   <li>any MaxMind or I/O exception occurs</li>
     * </ul>
     *
     * <p>This method never throws. Failures are logged and an empty result is returned
     * so that callers can continue email analysis uninterrupted.
     *
     * @param ipAddress a public IP address string (IPv4 or IPv6); may be null
     * @return a non-null {@link AsnResult}; fields are null when unresolvable
     */
    public AsnResult lookup(String ipAddress) {
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
            log.debug("ASN: could not parse IP address '{}': {}", ipAddress, e.getMessage());
            return emptyResult();
        }

        // Defense-in-depth: skip private/loopback/link-local addresses even though
        // EmailParserService.isPublicIp() should have already filtered them out.
        if (inetAddress.isLoopbackAddress()
                || inetAddress.isSiteLocalAddress()
                || inetAddress.isLinkLocalAddress()
                || inetAddress.isAnyLocalAddress()) {
            log.debug("ASN: skipping private/local address '{}'.", ipAddress);
            return emptyResult();
        }

        try {
            AsnResponse response = databaseReader.asn(inetAddress);
            // autonomousSystemNumber is a long in geoip2 4.x — format with AS prefix
            String asnNumber = response.getAutonomousSystemNumber() != null
                    ? "AS" + response.getAutonomousSystemNumber()
                    : null;
            String asnOrg = nullIfBlank(response.getAutonomousSystemOrganization());
            return AsnResult.builder()
                    .asnNumber(asnNumber)
                    .asnOrg(asnOrg)
                    .build();

        } catch (AddressNotFoundException e) {
            // Normal for IPs without an ASN record (some reserved ranges, private space).
            log.debug("ASN: no record found for IP '{}' in MaxMind ASN database.", ipAddress);
            return emptyResult();
        } catch (GeoIp2Exception e) {
            log.warn("ASN: MaxMind GeoIp2Exception for IP '{}': {}", ipAddress, e.getMessage());
            return emptyResult();
        } catch (IOException e) {
            log.warn("ASN: IOException during lookup for IP '{}': {}", ipAddress, e.getMessage());
            return emptyResult();
        }
    }

    private AsnResult emptyResult() {
        return AsnResult.builder().build();
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
