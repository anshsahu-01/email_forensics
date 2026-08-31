package com.emailForemsic.emailForensic.service;

import org.springframework.stereotype.Service;

@Service
public class GeoLocationService {

    public String getGeoLocation(String ipAddress) {
        // Location lookup logic here
        return "Unknown Location";
    }
}