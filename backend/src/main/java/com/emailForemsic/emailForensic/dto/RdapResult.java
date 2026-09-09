package com.emailForemsic.emailForensic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RdapResult {

    /**
     * IP address that was queried.
     */
    private String ipAddress;

    /**
     * RDAP server that answered the lookup.
     */
    private String rdapServer;

    /**
     * Registry/RIR inferred from the RDAP response.
     * Example: ARIN, RIPE NCC, APNIC, LACNIC, AFRINIC.
     */
    private String registry;

    /**
     * RDAP object handle.
     */
    private String handle;

    /**
     * Registered network/object name.
     */
    private String name;

    /**
     * Organization/network owner.
     */
    private String organization;

    /**
     * Country associated with the registered network.
     */
    private String country;

    /**
     * Beginning of the registered address range.
     */
    private String startAddress;

    /**
     * End of the registered address range.
     */
    private String endAddress;

    /**
     * CIDR/network range when available.
     */
    private String cidr;

    /**
     * Registration/last-changed events represented as readable values.
     */
    private List<RdapEvent> events;

    /**
     * Returns true when meaningful RDAP data was resolved.
     */
    public boolean isResolved() {
        return handle != null
                || name != null
                || organization != null
                || cidr != null
                || startAddress != null
                || endAddress != null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RdapEvent {

        private String eventAction;
        private String eventDate;
    }
}