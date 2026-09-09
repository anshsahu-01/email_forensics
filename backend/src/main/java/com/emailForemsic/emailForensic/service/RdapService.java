package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.RdapResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RdapService {

    private static final Logger log = LoggerFactory.getLogger(RdapService.class);

    private static final String IPV4_BOOTSTRAP =
            "https://data.iana.org/rdap/ipv4.json";

    private static final String IPV6_BOOTSTRAP =
            "https://data.iana.org/rdap/ipv6.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Looks up registration data for a public IP address.
     *
     * <p>Failures are intentionally non-fatal. RDAP is enrichment only and
     * must never prevent the email from being analyzed.</p>
     */
    public RdapResult lookup(String ipAddress) {

        if (ipAddress == null || ipAddress.isBlank()) {
            return emptyResult(ipAddress);
        }

        String normalizedIp = ipAddress.trim();

        // Defense-in-depth: RDAP accepts only literal IPv4/IPv6 addresses.
        // This prevents accidental hostname/DNS resolution through
        // InetAddress.getByName().
        if (!isIpLiteral(normalizedIp)) {
            log.debug(
                    "RDAP: rejecting non-literal IP address '{}'.",
                    ipAddress
            );
            return emptyResult(normalizedIp);
        }

        InetAddress address;

        try {
            address = InetAddress.getByName(normalizedIp);
        } catch (Exception e) {
            log.debug(
                    "RDAP: invalid IP address '{}'.",
                    ipAddress
            );
            return emptyResult(normalizedIp);
        }

        if (!isPublicAddress(address)) {
            log.debug(
                    "RDAP: skipping private/local IP '{}'.",
                    normalizedIp
            );
            return emptyResult(normalizedIp);
        }

        try {

            String bootstrapUrl = address.getAddress().length == 4
                    ? IPV4_BOOTSTRAP
                    : IPV6_BOOTSTRAP;

            String rdapBaseUrl = findRdapServer(
                    normalizedIp,
                    bootstrapUrl
            );

            if (rdapBaseUrl == null || rdapBaseUrl.isBlank()) {
                log.debug(
                        "RDAP: no authoritative server found for '{}'.",
                        normalizedIp
                );
                return emptyResult(normalizedIp);
            }

            String queryUrl = buildIpQueryUrl(
                    rdapBaseUrl,
                    normalizedIp
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(queryUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header(
                            "Accept",
                            "application/rdap+json, application/json"
                    )
                    .header(
                            "User-Agent",
                            "EmailForensics/1.0"
                    )
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                log.debug(
                        "RDAP: server returned HTTP {} for '{}'.",
                        response.statusCode(),
                        normalizedIp
                );

                return emptyResult(normalizedIp);
            }

            return parseResponse(
                    normalizedIp,
                    rdapBaseUrl,
                    response.body()
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            log.warn(
                    "RDAP: lookup interrupted for '{}'.",
                    normalizedIp
            );

            return emptyResult(normalizedIp);

        } catch (Exception e) {

            log.warn(
                    "RDAP: lookup failed for '{}': {}",
                    normalizedIp,
                    e.getMessage()
            );

            return emptyResult(normalizedIp);
        }
    }

    private String findRdapServer(
            String ipAddress,
            String bootstrapUrl
    ) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(bootstrapUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "EmailForensics/1.0")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            log.debug(
                    "RDAP bootstrap returned HTTP {}.",
                    response.statusCode()
            );

            return null;
        }

        JsonNode root =
                objectMapper.readTree(response.body());

        JsonNode services = root.get("services");

        if (services == null || !services.isArray()) {
            return null;
        }

        String bestServer = null;
        int bestPrefixLength = -1;

        InetAddress targetAddress =
                InetAddress.getByName(ipAddress);

        for (JsonNode service : services) {

            if (!service.isArray() || service.size() < 2) {
                continue;
            }

            JsonNode prefixes = service.get(0);
            JsonNode urls = service.get(1);

            if (!prefixes.isArray()
                    || !urls.isArray()
                    || urls.isEmpty()) {
                continue;
            }

            for (JsonNode prefixNode : prefixes) {

                String prefix = prefixNode.asText(null);

                if (prefix == null || prefix.isBlank()) {
                    continue;
                }

                int prefixLength = getPrefixLength(prefix);

                if (prefixLength < 0) {
                    continue;
                }

                if (prefixContains(
                        prefix,
                        targetAddress
                )) {

                    if (prefixLength > bestPrefixLength) {

                        String server =
                                urls.get(0).asText(null);

                        if (server != null
                                && !server.isBlank()) {

                            bestServer = server;
                            bestPrefixLength = prefixLength;
                        }
                    }
                }
            }
        }

        return bestServer;
    }

    private RdapResult parseResponse(
            String ipAddress,
            String rdapServer,
            String responseBody
    ) throws IOException {

        JsonNode root =
                objectMapper.readTree(responseBody);

        RdapResult.RdapResultBuilder builder =
                RdapResult.builder()
                        .ipAddress(ipAddress)
                        .rdapServer(rdapServer);

        builder.handle(
                textValue(root, "handle")
        );

        builder.name(
                textValue(root, "name")
        );

        builder.startAddress(
                textValue(root, "startAddress")
        );

        builder.endAddress(
                textValue(root, "endAddress")
        );

        builder.country(
                textValue(root, "country")
        );

        builder.cidr(
                extractCidr(root)
        );

        builder.organization(
                extractOrganization(root)
        );

        builder.registry(
                detectRegistry(rdapServer)
        );

        builder.events(
                extractEvents(root)
        );

        return builder.build();
    }

    private String extractOrganization(JsonNode root) {

        JsonNode entities = root.get("entities");

        if (entities == null || !entities.isArray()) {
            return null;
        }

        for (JsonNode entity : entities) {

            JsonNode roles = entity.get("roles");

            if (roles == null || !roles.isArray()) {
                continue;
            }

            boolean hasRegistrantRole = false;

            for (JsonNode role : roles) {

                if ("registrant".equalsIgnoreCase(
                        role.asText()
                )) {
                    hasRegistrantRole = true;
                    break;
                }
            }

            if (!hasRegistrantRole) {
                continue;
            }

            JsonNode vcardArray =
                    entity.get("vcardArray");

            String organization =
                    extractVcardValue(
                            vcardArray,
                            "fn"
                    );

            if (organization == null) {
                organization =
                        extractVcardValue(
                                vcardArray,
                                "org"
                        );
            }

            if (organization != null) {
                return organization;
            }
        }

        return null;
    }

    private String extractVcardValue(
            JsonNode vcardArray,
            String property
    ) {

        if (vcardArray == null
                || !vcardArray.isArray()
                || vcardArray.size() < 2) {
            return null;
        }

        JsonNode properties =
                vcardArray.get(1);

        if (!properties.isArray()) {
            return null;
        }

        for (JsonNode item : properties) {

            if (!item.isArray()
                    || item.size() < 4) {
                continue;
            }

            if (property.equalsIgnoreCase(
                    item.get(0).asText()
            )) {

                return item.get(3).asText(null);
            }
        }

        return null;
    }

    private List<RdapResult.RdapEvent> extractEvents(
            JsonNode root
    ) {

        JsonNode events = root.get("events");

        if (events == null || !events.isArray()) {
            return Collections.emptyList();
        }

        List<RdapResult.RdapEvent> result =
                new ArrayList<>();

        for (JsonNode event : events) {

            String action =
                    textValue(event, "eventAction");

            String date =
                    textValue(event, "eventDate");

            if (action != null || date != null) {

                result.add(
                        RdapResult.RdapEvent.builder()
                                .eventAction(action)
                                .eventDate(date)
                                .build()
                );
            }
        }

        return result;
    }

    private String extractCidr(JsonNode root) {

        JsonNode cidr0Cidr =
                root.path("cidr0_cidrs");

        if (cidr0Cidr.isArray()
                && !cidr0Cidr.isEmpty()) {

            JsonNode first =
                    cidr0Cidr.get(0);

            String v4prefix =
                    textValue(first, "v4prefix");

            if (v4prefix != null) {

                JsonNode length =
                        first.get("length");

                if (length != null) {

                    return v4prefix
                            + "/"
                            + length.asText();
                }

                return v4prefix;
            }

            String v6prefix =
                    textValue(first, "v6prefix");

            if (v6prefix != null) {

                JsonNode length =
                        first.get("length");

                if (length != null) {

                    return v6prefix
                            + "/"
                            + length.asText();
                }

                return v6prefix;
            }
        }

        return null;
    }

    private String detectRegistry(String rdapServer) {

        if (rdapServer == null) {
            return null;
        }

        String lower =
                rdapServer.toLowerCase();

        if (lower.contains("arin")) {
            return "ARIN";
        }

        if (lower.contains("ripe")) {
            return "RIPE NCC";
        }

        if (lower.contains("apnic")) {
            return "APNIC";
        }

        if (lower.contains("lacnic")) {
            return "LACNIC";
        }

        if (lower.contains("afrinic")) {
            return "AFRINIC";
        }

        return null;
    }

    private String buildIpQueryUrl(
            String baseUrl,
            String ipAddress
    ) {

        String normalizedBase =
                baseUrl.endsWith("/")
                        ? baseUrl
                        : baseUrl + "/";

        return normalizedBase
                + "ip/"
                + ipAddress;
    }

    private int getPrefixLength(String cidr) {

        int slashIndex =
                cidr.indexOf('/');

        if (slashIndex < 0) {
            return -1;
        }

        try {
            return Integer.parseInt(
                    cidr.substring(slashIndex + 1)
            );
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean prefixContains(
            String cidr,
            InetAddress target
    ) {

        try {

            String[] parts =
                    cidr.split("/", 2);

            if (parts.length != 2) {
                return false;
            }

            InetAddress network =
                    InetAddress.getByName(parts[0]);

            int prefixLength =
                    Integer.parseInt(parts[1]);

            byte[] networkBytes =
                    network.getAddress();

            byte[] targetBytes =
                    target.getAddress();

            if (networkBytes.length
                    != targetBytes.length) {
                return false;
            }

            int fullBytes =
                    prefixLength / 8;

            int remainingBits =
                    prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {

                if (networkBytes[i]
                        != targetBytes[i]) {
                    return false;
                }
            }

            if (remainingBits > 0) {

                int mask =
                        0xFF << (8 - remainingBits);

                int networkValue =
                        networkBytes[fullBytes] & mask;

                int targetValue =
                        targetBytes[fullBytes] & mask;

                if (networkValue != targetValue) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {

            log.debug(
                    "RDAP: invalid bootstrap prefix '{}'.",
                    cidr
            );

            return false;
        }
    }

    /**
     * Checks whether the supplied value is a literal IPv4 or IPv6 address.
     *
     * <p>This deliberately does not perform DNS resolution. RDAP should never
     * receive a hostname and resolve it implicitly.</p>
     */
    private boolean isIpLiteral(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        String candidate = value.trim();

        // IPv4 literal
        if (candidate.matches(
                "^(?:\\d{1,3}\\.){3}\\d{1,3}$"
        )) {

            String[] octets = candidate.split("\\.");

            for (String octet : octets) {

                try {

                    int valuePart = Integer.parseInt(octet);

                    if (valuePart < 0 || valuePart > 255) {
                        return false;
                    }

                } catch (NumberFormatException e) {
                    return false;
                }
            }

            return true;
        }

        // IPv6 literal must contain ':'.
        // InetAddress is only used after this literal check.
        if (candidate.contains(":")) {

            try {

                InetAddress address =
                        InetAddress.getByName(candidate);

                return address.getHostAddress() != null
                        && address.getAddress().length == 16;

            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    private boolean isPublicAddress(
            InetAddress address
    ) {

        return !address.isAnyLocalAddress()
                && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress()
                && !address.isSiteLocalAddress()
                && !address.isMulticastAddress();
    }

    private String textValue(
            JsonNode node,
            String field
    ) {

        JsonNode value = node.get(field);

        if (value == null
                || value.isNull()
                || value.asText().isBlank()) {
            return null;
        }

        return value.asText();
    }

    private RdapResult emptyResult(String ipAddress) {

        return RdapResult.builder()
                .ipAddress(ipAddress)
                .build();
    }
}