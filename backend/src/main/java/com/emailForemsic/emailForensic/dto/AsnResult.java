package com.emailForemsic.emailForensic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carries the ASN / network intelligence fields resolved from a MaxMind
 * GeoLite2-ASN database lookup.
 * <p>
 * GeoLite2-ASN provides exactly two data points per IP address:
 * the autonomous system number and the organization name registered to that AS.
 * <p>
 * All fields are nullable — an empty result is valid when the database is not
 * configured, the IP is private/unresolvable, or no record exists for the address.
 * <p>
 * Note: ASN data identifies network ownership, not physical location or individual
 * identity. Use wording such as "Network Intelligence" in display contexts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsnResult {

    /**
     * Autonomous System Number in prefixed string form, e.g. {@code "AS15169"}.
     * Null when unresolvable.
     */
    private String asnNumber;

    /**
     * Organization name registered to the AS, e.g. {@code "GOOGLE"}.
     * Directly from MaxMind {@code autonomousSystemOrganization}. Null when unresolvable.
     */
    private String asnOrg;
}
