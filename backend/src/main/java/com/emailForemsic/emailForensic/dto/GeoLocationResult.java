package com.emailForemsic.emailForensic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carries the relevant geolocation fields resolved from a MaxMind GeoIP2 database lookup.
 * <p>
 * All fields are nullable — a partial or completely empty result is valid when MaxMind
 * cannot resolve the address or when the database is not configured.
 * <p>
 * Coordinates represent approximate IP geolocation only; they do not identify the
 * exact physical location of an individual.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationResult {

    /**
     * ISO country name, e.g. "United States". Null when unresolvable.
     */
    private String country;

    /**
     * City name, e.g. "Mountain View". Null when unresolvable.
     */
    private String city;

    /**
     * Approximate latitude in decimal degrees. Null when unresolvable.
     */
    private Double latitude;

    /**
     * Approximate longitude in decimal degrees. Null when unresolvable.
     */
    private Double longitude;

    /**
     * IANA timezone identifier, e.g. "America/Los_Angeles". Null when unresolvable.
     */
    private String timezone;
}
