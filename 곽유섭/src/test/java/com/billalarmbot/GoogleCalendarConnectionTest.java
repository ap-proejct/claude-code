package com.billalarmbot;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.util.List;

class GoogleCalendarConnectionTest {

    private static final String CALENDAR_ID = "908d3619acc12abee1a9454cfc1831708102b4c7630386b0c51bf6dda951f4e8@group.calendar.google.com";
    private static final String CREDENTIALS_PATH = "src/main/resources/billalarmbot-88f7fdf6bc4b.json";

    @Test
    void calendar_연결_및_일정_생성() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(CREDENTIALS_PATH))
                .createScoped(List.of(CalendarScopes.CALENDAR));

        Calendar calendar = new Calendar.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("BillAlarmBot")
                .build();

        Event event = new Event()
                .setSummary("[BillAlarmBot] Netflix 정산 D-7 알림")
                .setDescription("Netflix 구독 정산일 7일 전 알림\n금액: 17,000원\n결제수단: 신용카드");

        EventDateTime start = new EventDateTime()
                .setDate(new DateTime("2026-04-15"))
                .setTimeZone("Asia/Seoul");

        EventDateTime end = new EventDateTime()
                .setDate(new DateTime("2026-04-15"))
                .setTimeZone("Asia/Seoul");

        event.setStart(start).setEnd(end);

        Event created = calendar.events().insert(CALENDAR_ID, event).execute();

        System.out.println("=== Google Calendar 일정 생성 성공 ===");
        System.out.println("일정 ID: " + created.getId());
        System.out.println("일정 제목: " + created.getSummary());
        System.out.println("일정 링크: " + created.getHtmlLink());
    }
}
