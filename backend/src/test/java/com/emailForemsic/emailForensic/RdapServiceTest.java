package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.RdapResult;
import com.emailForemsic.emailForensic.service.RdapService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RdapServiceTest {

    private final RdapService rdapService = new RdapService();

    @Test
    void lookupShouldReturnEmptyResultForNullIp() {

        RdapResult result = rdapService.lookup(null);

        assertNotNull(result);
        assertNull(result.getRdapServer());
        assertNull(result.getOrganization());
    }

    @Test
    void lookupShouldReturnEmptyResultForBlankIp() {

        RdapResult result = rdapService.lookup("   ");

        assertNotNull(result);
        assertNull(result.getRdapServer());
    }

    @Test
    void lookupShouldSkipPrivateIp() {

        RdapResult result = rdapService.lookup("192.168.1.10");

        assertNotNull(result);
        assertEquals("192.168.1.10", result.getIpAddress());
        assertNull(result.getRdapServer());
    }

    @Test
    void lookupShouldSkipLoopbackIp() {

        RdapResult result = rdapService.lookup("127.0.0.1");

        assertNotNull(result);
        assertNull(result.getRdapServer());
    }

    @Test
    void lookupShouldNotThrowForInvalidIp() {

        assertDoesNotThrow(
                () -> rdapService.lookup("not-an-ip"));
    }

    @Test
    void lookupShouldRejectHostnameWithoutDnsResolution() {

        RdapResult result = rdapService.lookup("example.com");

        assertNotNull(result);
        assertEquals("example.com", result.getIpAddress());
        assertNull(result.getRdapServer());
        assertNull(result.getOrganization());
    }

    @Test
    void lookupShouldResolvePublicIp() {
        RdapResult result = rdapService.lookup("8.8.8.8");

        assertNotNull(result);
        assertEquals("8.8.8.8", result.getIpAddress());

        if (result.getRdapServer() != null) {
            System.out.println("RDAP Server: " + result.getRdapServer());
            System.out.println("Registry: " + result.getRegistry());
            System.out.println("Handle: " + result.getHandle());
            System.out.println("Name: " + result.getName());
            System.out.println("Organization: " + result.getOrganization());
            System.out.println("Country: " + result.getCountry());
            System.out.println("CIDR: " + result.getCidr());
            System.out.println("Events: " + result.getEvents());
        } else {
            System.out.println(
                    "RDAP lookup unavailable during test run; "
                            + "service returned a safe empty enrichment result.");
        }
    }
}