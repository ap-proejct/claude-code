package com.billalarmbot.service;

import com.billalarmbot.client.GoogleCalendarClient;
import com.billalarmbot.client.TelegramClient;
import com.billalarmbot.domain.NotificationHistory;
import com.billalarmbot.domain.Subscription;
import com.billalarmbot.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final GoogleCalendarClient googleCalendarClient;
    private final TelegramClient telegramClient;
    private final NotificationHistoryRepository notificationHistoryRepository;

    public void notify(Subscription subscription, LocalDate billingDate) {
        LocalDate billingMonth = billingDate.withDayOfMonth(1);

        // 1차 확인: DB 이력
        if (notificationHistoryRepository.existsByServiceNameAndBillingMonth(
                subscription.getServiceName(), billingMonth)) {

            // 이미 알림 발송됨 → Calendar 일정이 삭제됐는지만 확인 후 복구
            if (!googleCalendarClient.existsEvent(subscription, billingDate)) {
                log.info("Calendar 일정 삭제 감지, 재생성 — 서비스명: {}", subscription.getServiceName());
                googleCalendarClient.createEvent(subscription, billingDate);
            } else {
                log.info("중복 skip — 서비스명: {}, billingMonth: {}",
                        subscription.getServiceName(), billingMonth);
            }
            return;
        }

        // 신규 알림 발송
        googleCalendarClient.createEvent(subscription, billingDate);
        log.info("Calendar 일정 생성 완료 — {}", subscription.getServiceName());

        telegramClient.sendMessage(buildMessage(subscription, billingDate));
        log.info("Telegram 메시지 발송 완료 — {}", subscription.getServiceName());

        saveHistory(subscription, billingMonth);
        log.info("알림 이력 저장 완료 — {}", subscription.getServiceName());
    }

    private void saveHistory(Subscription subscription, LocalDate billingMonth) {
        notificationHistoryRepository.save(NotificationHistory.builder()
                .serviceName(subscription.getServiceName())
                .billingDay(subscription.getBillingDay())
                .billingMonth(billingMonth)
                .notifiedAt(LocalDateTime.now())
                .build());
    }

    private String buildMessage(Subscription subscription, LocalDate billingDate) {
        return String.format(
                "\uD83D\uDD14 <b>정산 D-7 알림</b>%n%n"
                + "서비스명: %s%n"
                + "금액: %,d원%n"
                + "정산일: %s%n"
                + "결제수단: %s",
                subscription.getServiceName(),
                subscription.getAmount(),
                billingDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                subscription.getPaymentMethod()
        );
    }
}
