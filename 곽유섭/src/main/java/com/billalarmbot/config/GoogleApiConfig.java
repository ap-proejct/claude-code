package com.billalarmbot.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
@Configuration
public class GoogleApiConfig {

    private static final String APPLICATION_NAME = "BillAlarmBot";

    @Value("${google.credentials.path}")
    private String credentialsPath;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        log.info("Loading Google credentials from classpath: {}", credentialsPath);
        ClassPathResource resource = new ClassPathResource(credentialsPath);
        try (InputStream inputStream = resource.getInputStream()) {
            return GoogleCredentials
                    .fromStream(inputStream)
                    .createScoped(List.of(SheetsScopes.SPREADSHEETS_READONLY, CalendarScopes.CALENDAR));
        }
    }

    @Bean
    public Sheets sheetsService(GoogleCredentials googleCredentials)
            throws GeneralSecurityException, IOException {
        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(googleCredentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Bean
    public Calendar calendarService(GoogleCredentials googleCredentials)
            throws GeneralSecurityException, IOException {
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(googleCredentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
