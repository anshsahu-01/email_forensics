package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.AsnResult;
import com.emailForemsic.emailForensic.service.AsnService;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsnServiceTest {

    private AsnService asnService;
    private DatabaseReader databaseReader;

    @BeforeEach
    void setUp() throws Exception {
        asnService = new AsnService();
        databaseReader = mock(DatabaseReader.class);
        // Inject mock reader directly — bypasses @PostConstruct file loading
        setField(asnService, "databaseReader", databaseReader);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // -----------------------------------------------------------------------
    // Helper — build a mock AsnResponse with the given values
    // -----------------------------------------------------------------------
    private AsnResponse buildAsnResponse(Long asnNumber, String org) {
        AsnResponse response = mock(AsnResponse.class);
        when(response.getAutonomousSystemNumber()).thenReturn(asnNumber);
        when(response.getAutonomousSystemOrganization()).thenReturn(org);
        return response;
    }

    // -----------------------------------------------------------------------
    // 1. Valid public IPv4 — successful lookup returns populated result
    // -----------------------------------------------------------------------
    @Test
    void returnsPopulatedResultForValidPublicIPv4() throws Exception {
        AsnResponse asnResponse = buildAsnResponse(15169L, "GOOGLE");
        when(databaseReader.asn(any(InetAddress.class))).thenReturn(asnResponse);

        AsnResult result = asnService.lookup("8.8.8.8");

        assertNotNull(result);
        assertEquals("AS15169", result.getAsnNumber());
        assertEquals("GOOGLE", result.getAsnOrg());
        verify(databaseReader, times(1)).asn(any(InetAddress.class));
    }

    // -----------------------------------------------------------------------
    // 2. Valid public IPv6 — successful lookup
    // -----------------------------------------------------------------------
    @Test
    void returnsPopulatedResultForValidPublicIPv6() throws Exception {
        AsnResponse asnResponse = buildAsnResponse(32934L, "FACEBOOK");
        when(databaseReader.asn(any(InetAddress.class))).thenReturn(asnResponse);

        AsnResult result = asnService.lookup("2a03:2880:f003:c07:face:b00c::2");

        assertNotNull(result);
        assertEquals("AS32934", result.getAsnNumber());
        assertEquals("FACEBOOK", result.getAsnOrg());
        verify(databaseReader, times(1)).asn(any(InetAddress.class));
    }

    // -----------------------------------------------------------------------
    // 3. Private IPv4 (10.x.x.x) — must not reach database reader
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForPrivateIPv4WithoutDatabaseCall() throws Exception {
        AsnResult result = asnService.lookup("10.0.0.1");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 4. Site-local IPv4 (192.168.x.x) — must not reach database reader
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForSiteLocalIPv4WithoutDatabaseCall() throws Exception {
        AsnResult result = asnService.lookup("192.168.1.100");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 5. Loopback IPv4 (127.0.0.1) — must not reach database reader
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForLoopbackIPv4WithoutDatabaseCall() throws Exception {
        AsnResult result = asnService.lookup("127.0.0.1");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 6. Loopback IPv6 (::1) — must not reach database reader
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForLoopbackIPv6WithoutDatabaseCall() throws Exception {
        AsnResult result = asnService.lookup("::1");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 7. Null IP — returns empty result without database call
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForNullIp() throws Exception {
        AsnResult result = asnService.lookup(null);

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 8. Blank IP — returns empty result without database call
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForBlankIp() throws Exception {
        AsnResult result = asnService.lookup("   ");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 9. IP with no ASN record (AddressNotFoundException) — non-fatal
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultWhenAddressNotFoundInDatabase() throws Exception {
        when(databaseReader.asn(any(InetAddress.class)))
                .thenThrow(new AddressNotFoundException("No record for address"));

        AsnResult result = asnService.lookup("203.0.113.1");

        assertEmptyResult(result);
        // Must not propagate exception
    }

    // -----------------------------------------------------------------------
    // 10. MaxMind GeoIp2Exception — non-fatal, returns empty result
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultOnGeoIp2Exception() throws Exception {
        when(databaseReader.asn(any(InetAddress.class)))
                .thenThrow(new GeoIp2Exception("database error"));

        AsnResult result = asnService.lookup("203.0.113.1");

        assertEmptyResult(result);
    }

    // -----------------------------------------------------------------------
    // 11. IOException during lookup — non-fatal, returns empty result
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultOnIOException() throws Exception {
        when(databaseReader.asn(any(InetAddress.class)))
                .thenThrow(new IOException("read error"));

        AsnResult result = asnService.lookup("203.0.113.1");

        assertEmptyResult(result);
    }

    // -----------------------------------------------------------------------
    // 12. DatabaseReader is null (database path not configured) — empty result
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultWhenDatabaseReaderIsNull() throws Exception {
        setField(asnService, "databaseReader", null);

        AsnResult result = asnService.lookup("8.8.8.8");

        assertEmptyResult(result);
    }

    // -----------------------------------------------------------------------
    // 13. init() with blank path — databaseReader stays null, no exception
    // -----------------------------------------------------------------------
    @Test
    void initWithBlankPathLeavesReaderNullWithoutException() throws Exception {
        AsnService service = new AsnService();
        setField(service, "databasePath", "");
        setField(service, "databaseReader", null);

        java.lang.reflect.Method initMethod = AsnService.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        assertDoesNotThrow((org.junit.jupiter.api.function.Executable) () -> initMethod.invoke(service));

        Field readerField = AsnService.class.getDeclaredField("databaseReader");
        readerField.setAccessible(true);
        assertNull(readerField.get(service));
    }

    // -----------------------------------------------------------------------
    // 14. init() with non-existent path — databaseReader stays null, no exception
    // -----------------------------------------------------------------------
    @Test
    void initWithNonExistentPathLeavesReaderNullWithoutException() throws Exception {
        AsnService service = new AsnService();
        setField(service, "databasePath", "/nonexistent/path/GeoLite2-ASN.mmdb");
        setField(service, "databaseReader", null);

        java.lang.reflect.Method initMethod = AsnService.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        assertDoesNotThrow((org.junit.jupiter.api.function.Executable) () -> initMethod.invoke(service));

        Field readerField = AsnService.class.getDeclaredField("databaseReader");
        readerField.setAccessible(true);
        assertNull(readerField.get(service));
    }

    // -----------------------------------------------------------------------
    // 15. AS number is correctly prefixed with "AS"
    // -----------------------------------------------------------------------
    @Test
    void prefixesAsnNumberWithAS() throws Exception {
        AsnResponse asnResponse = buildAsnResponse(16509L, "AMAZON-02");
        when(databaseReader.asn(any(InetAddress.class))).thenReturn(asnResponse);

        AsnResult result = asnService.lookup("54.239.28.85");

        assertEquals("AS16509", result.getAsnNumber());
        assertEquals("AMAZON-02", result.getAsnOrg());
    }

    // -----------------------------------------------------------------------
    // Helper assertion
    // -----------------------------------------------------------------------
    private void assertEmptyResult(AsnResult result) {
        assertNotNull(result);
        assertNull(result.getAsnNumber());
        assertNull(result.getAsnOrg());
    }
}
