package com.billalarmbot;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.util.List;

class GoogleSheetsConnectionTest {

    private static final String SPREADSHEET_ID = "1AHgl2U2T1sduE11QDm_dUVybtzSbPemvfiFKm29nPko";
    private static final String CREDENTIALS_PATH = "src/main/resources/billalarmbot-88f7fdf6bc4b.json";

    @Test
    void sheets_연결_및_데이터_읽기() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(CREDENTIALS_PATH))
                .createScoped(List.of(SheetsScopes.SPREADSHEETS_READONLY));

        Sheets sheets = new Sheets.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("BillAlarmBot")
                .build();

        ValueRange response = sheets.spreadsheets().values()
                .get(SPREADSHEET_ID, "A1:D10")
                .execute();

        List<List<Object>> values = response.getValues();

        System.out.println("=== Google Sheets 데이터 ===");
        if (values == null || values.isEmpty()) {
            System.out.println("데이터 없음");
        } else {
            values.forEach(row -> System.out.println(row));
        }
    }
}
