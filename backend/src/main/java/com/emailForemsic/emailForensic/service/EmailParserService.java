package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class EmailParserService {

    public EmailParsedResult parseEml(InputStream inputStream) {
        // Parsing logic here
        EmailParsedResult result = new EmailParsedResult();
        return result;
    }
}