package com.billalarmbot.client;

import com.billalarmbot.domain.Subscription;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarClientImpl implements GoogleCalendarClient {

    private static final String TIME_ZONE = "Asia/Seoul";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Calendar calendarService;

    @Value("${google.calendar.id}")
    private String calendarId;

    @Override
    public void createEvent(Subscription subscription, LocalDate billingDate) {
        log.info("Creating calendar event: service={}, billingDate={}", subscription.getServiceName(), billingDate);

        Event event = new Event()
                .setSummary(buildTitle(subscription.getServiceName()))
                .setDescription(buildDescription(subscription, billingDate));

        EventDateTime eventDate = new EventDateTime()
                .setDate(new DateTime(billingDate.format(DATE_FORMATTER)))
                .setTimeZone(TIME_ZONE);

        event.setStart(eventDate).setEnd(eventDate);

        try {
            Event created = calendarService.events().insert(calendarId, event).execute();
            log.info("Calendar event created: id={}", created.getId());
        } catch (IOException e) {
            log.error("Failed to create calendar event for: {}", subscription.getServiceName(), e);
            throw new RuntimeException("Google Calendar API call failed", e);
        }
    }

    @Override
    public boolean existsEvent(Subscription subscription, LocalDate billingDate) {
        String title = buildTitle(subscription.getServiceName());
        // 해당 정산일 하루 범위를 검색 (종일 이벤트는 시간이 없으므로 날짜 기준)
        String timeMin = billingDate.format(DATE_FORMATTER) + "T00:00:00+09:00";
        String timeMax = billingDate.plusDays(1).format(DATE_FORMATTER) + "T00:00:00+09:00";

        log.info("Checking calendar event: service={}, billingDate={}", subscription.getServiceName(), billingDate);

        try {
            Events events = calendarService.events().list(calendarId)
                    .setQ(title)
                    .setTimeMin(new DateTime(timeMin))
                    .setTimeMax(new DateTime(timeMax))
                    .setSingleEvents(true)
                    .execute();

            boolean exists = events.getItems() != null && !events.getItems().isEmpty();
            log.info("Calendar event exists={} for service={}", exists, subscription.getServiceName());
            return exists;

        } catch (IOException e) {
            // Calendar 조회 실패 시 안전하게 false 반환 (이후 DB 이력으로만 판단)
            log.warn("Failed to check calendar event for: {}, proceeding without calendar check",
                    subscription.getServiceName(), e);
            return false;
        }
    }

    private String buildTitle(String serviceName) {
        return String.format("[BillAlarmBot] %s 정산 D-7 알림", serviceName);
    }

    private String buildDescription(Subscription subscription, LocalDate billingDate) {
        return String.format(
                "서비스명: %s%n금액: %,d원%n정산일: %s%n결제수단: %s",
                subscription.getServiceName(),
                subscription.getAmount(),
                billingDate.format(DATE_FORMATTER),
                subscription.getPaymentMethod()
        );
    }
}
