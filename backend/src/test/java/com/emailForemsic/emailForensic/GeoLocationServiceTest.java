package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.GeoLocationResult;
import com.emailForemsic.emailForensic.service.GeoLocationService;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeoLocationServiceTest {

    private GeoLocationService geoLocationService;
    private DatabaseReader databaseReader;

    @BeforeEach
    void setUp() throws Exception {
        geoLocationService = new GeoLocationService();
        databaseReader = mock(DatabaseReader.class);
        // Inject mock reader directly — bypasses @PostConstruct file loading
        setField(geoLocationService, "databaseReader", databaseReader);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // -----------------------------------------------------------------------
    // Helper — build a CityResponse with the given values
    // -----------------------------------------------------------------------
    private CityResponse buildCityResponse(String countryName, String cityName,
                                           Double latitude, Double longitude,
                                           String timezone) {
        Country country = mock(Country.class);
        when(country.getName()).thenReturn(countryName);

        City city = mock(City.class);
        when(city.getName()).thenReturn(cityName);

        Location location = mock(Location.class);
        when(location.getLatitude()).thenReturn(latitude);
        when(location.getLongitude()).thenReturn(longitude);
        when(location.getTimeZone()).thenReturn(timezone);

        CityResponse response = mock(CityResponse.class);
        when(response.getCountry()).thenReturn(country);
        when(response.getCity()).thenReturn(city);
        when(response.getLocation()).thenReturn(location);
        return response;
    }

    // -----------------------------------------------------------------------
    // 1. Valid public IPv4 — successful lookup returns populated result
    // -----------------------------------------------------------------------
    @Test
    void returnsPopulatedResultForValidPublicIPv4() throws Exception {
        CityResponse cityResponse = buildCityResponse(
                "United States", "Mountain View", 37.386, -122.0838, "America/Los_Angeles");
        when(databaseReader.city(any(InetAddress.class))).thenReturn(cityResponse);

        GeoLocationResult result = geoLocationService.lookup("8.8.8.8");

        assertNotNull(result);
        assertEquals("United States", result.getCountry());
        assertEquals("Mountain View", result.getCity());
        assertEquals(37.386, result.getLatitude());
        assertEquals(-122.0838, result.getLongitude());
        assertEquals("America/Los_Angeles", result.getTimezone());
        verify(databaseReader, times(1)).city(any(InetAddress.class));
    }

    // -----------------------------------------------------------------------
    // 2. Valid public IPv6 — successful lookup
    // -----------------------------------------------------------------------
    @Test
    void returnsPopulatedResultForValidPublicIPv6() throws Exception {
        CityResponse cityResponse = buildCityResponse(
                "Germany", "Frankfurt am Main", 50.1109, 8.6821, "Europe/Berlin");
        when(databaseReader.city(any(InetAddress.class))).thenReturn(cityResponse);

        GeoLocationResult result = geoLocationService.lookup("2001:4860:4860::8888");

        assertNotNull(result);
        assertEquals("Germany", result.getCountry());
        assertEquals("Frankfurt am Main", result.getCity());
        assertEquals(50.1109, result.getLatitude());
        assertEquals(8.6821, result.getLongitude());
        assertEquals("Europe/Berlin", result.getTimezone());
        verify(databaseReader, times(1)).city(any(InetAddress.class));
    }

    // -----------------------------------------------------------------------
    // 3. Private IPv4 (10.x.x.x) — must not reach database reader
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForPrivateIPv4WithoutDatabaseCall() throws Exception {
        GeoLocationResult result = geoLocationService.lookup("10.0.0.1");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 4. Private IPv4 (192.168.x.x) — site-local, must not reach database reader
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForSiteLocalIPv4WithoutDatabaseCall() throws Exception {
        GeoLocationResult result = geoLocationService.lookup("192.168.1.100");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 5. Loopback IPv4 (127.0.0.1) — must not reach database reader
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForLoopbackIPv4WithoutDatabaseCall() throws Exception {
        GeoLocationResult result = geoLocationService.lookup("127.0.0.1");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 6. Loopback IPv6 (::1) — must not reach database reader
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForLoopbackIPv6WithoutDatabaseCall() throws Exception {
        GeoLocationResult result = geoLocationService.lookup("::1");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 7. Null IP — returns empty result without database call
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForNullIp() throws Exception {
        GeoLocationResult result = geoLocationService.lookup(null);

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 8. Blank/empty IP — returns empty result without database call
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultForBlankIp() throws Exception {
        GeoLocationResult result = geoLocationService.lookup("   ");

        assertEmptyResult(result);
        verifyNoInteractions(databaseReader);
    }

    // -----------------------------------------------------------------------
    // 9. IP with no MaxMind record (AddressNotFoundException) — non-fatal
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultWhenAddressNotFoundInDatabase() throws Exception {
        when(databaseReader.city(any(InetAddress.class)))
                .thenThrow(new AddressNotFoundException("No record for address"));

        GeoLocationResult result = geoLocationService.lookup("203.0.113.1");

        assertEmptyResult(result);
        // Must not propagate exception
    }

    // -----------------------------------------------------------------------
    // 10. MaxMind GeoIp2Exception — non-fatal, returns empty result
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultOnGeoIp2Exception() throws Exception {
        when(databaseReader.city(any(InetAddress.class)))
                .thenThrow(new GeoIp2Exception("database error"));

        GeoLocationResult result = geoLocationService.lookup("203.0.113.1");

        assertEmptyResult(result);
    }

    // -----------------------------------------------------------------------
    // 11. IOException during lookup — non-fatal, returns empty result
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultOnIOException() throws Exception {
        when(databaseReader.city(any(InetAddress.class)))
                .thenThrow(new IOException("read error"));

        GeoLocationResult result = geoLocationService.lookup("203.0.113.1");

        assertEmptyResult(result);
    }

    // -----------------------------------------------------------------------
    // 12. DatabaseReader is null (database path not configured) — empty result
    // -----------------------------------------------------------------------
    @Test
    void returnsEmptyResultWhenDatabaseReaderIsNull() throws Exception {
        setField(geoLocationService, "databaseReader", null);

        GeoLocationResult result = geoLocationService.lookup("8.8.8.8");

        assertEmptyResult(result);
    }

    // -----------------------------------------------------------------------
    // 13. init() with blank path — databaseReader stays null, no exception
    // -----------------------------------------------------------------------
    @Test
    void initWithBlankPathLeavesReaderNullWithoutException() throws Exception {
        GeoLocationService service = new GeoLocationService();
        setField(service, "databasePath", "");
        setField(service, "databaseReader", null);

        // Call init() via reflection since it is package-private
        java.lang.reflect.Method initMethod = GeoLocationService.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        // Must not throw
        assertDoesNotThrow((org.junit.jupiter.api.function.Executable) () -> initMethod.invoke(service));

        Field readerField = GeoLocationService.class.getDeclaredField("databaseReader");
        readerField.setAccessible(true);
        assertNull(readerField.get(service));
    }

    // -----------------------------------------------------------------------
    // 14. init() with non-existent path — databaseReader stays null, no exception
    // -----------------------------------------------------------------------
    @Test
    void initWithNonExistentPathLeavesReaderNullWithoutException() throws Exception {
        GeoLocationService service = new GeoLocationService();
        setField(service, "databasePath", "/nonexistent/path/GeoLite2-City.mmdb");
        setField(service, "databaseReader", null);

        java.lang.reflect.Method initMethod = GeoLocationService.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        // Must not throw
        assertDoesNotThrow((org.junit.jupiter.api.function.Executable) () -> initMethod.invoke(service));

        Field readerField = GeoLocationService.class.getDeclaredField("databaseReader");
        readerField.setAccessible(true);
        assertNull(readerField.get(service));
    }

    // -----------------------------------------------------------------------
    // 15. Partial result — country present, city/timezone null (blank) → null fields
    // -----------------------------------------------------------------------
    @Test
    void nullifiesBlankCityAndTimezone() throws Exception {
        CityResponse cityResponse = buildCityResponse("Japan", "", 35.6762, 139.6503, "");
        when(databaseReader.city(any(InetAddress.class))).thenReturn(cityResponse);

        GeoLocationResult result = geoLocationService.lookup("103.9.68.1");

        assertEquals("Japan", result.getCountry());
        assertNull(result.getCity());       // blank → null
        assertNull(result.getTimezone());   // blank → null
        assertEquals(35.6762, result.getLatitude());
        assertEquals(139.6503, result.getLongitude());
    }

    // -----------------------------------------------------------------------
    // Helper assertion
    // -----------------------------------------------------------------------
    private void assertEmptyResult(GeoLocationResult result) {
        assertNotNull(result);
        assertNull(result.getCountry());
        assertNull(result.getCity());
        assertNull(result.getLatitude());
        assertNull(result.getLongitude());
        assertNull(result.getTimezone());
    }
}
