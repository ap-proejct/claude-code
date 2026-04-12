package com.billalarmbot.service;

import com.billalarmbot.client.GoogleCalendarClient;
import com.billalarmbot.client.TelegramClient;
import com.billalarmbot.domain.NotificationHistory;
import com.billalarmbot.domain.Subscription;
import com.billalarmbot.repository.NotificationHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private GoogleCalendarClient googleCalendarClient;

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @InjectMocks
    private NotificationService notificationService;

    private final Subscription subscription = Subscription.builder()
            .serviceName("Netflix")
            .amount(17000)
            .billingDay(19)
            .paymentMethod("신용카드")
            .build();

    private final LocalDate billingDate = LocalDate.of(2026, 4, 19);
    private final LocalDate billingMonth = LocalDate.of(2026, 4, 1);

    // ─── 신규 알림 (이력 없음) ────────────────────────────────

    @Test
    void DB_이력이_없으면_Calendar_생성_Telegram_발송_DB_저장이_모두_실행된다() {
        when(notificationHistoryRepository.existsByServiceNameAndBillingMonth("Netflix", billingMonth))
                .thenReturn(false);

        notificationService.notify(subscription, billingDate);

        verify(googleCalendarClient).createEvent(subscription, billingDate);
        verify(telegramClient).sendMessage(anyString());
        verify(notificationHistoryRepository).save(any(NotificationHistory.class));
    }

    @Test
    void DB_저장시_serviceName과_billingMonth가_올바르게_저장된다() {
        when(notificationHistoryRepository.existsByServiceNameAndBillingMonth("Netflix", billingMonth))
                .thenReturn(false);

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);

        notificationService.notify(subscription, billingDate);

        verify(notificationHistoryRepository).save(captor.capture());
        NotificationHistory saved = captor.getValue();
        assertThat(saved.getServiceName()).isEqualTo("Netflix");
        assertThat(saved.getBillingMonth()).isEqualTo(billingMonth);
    }

    @Test
    void Telegram_메시지에_서비스명과_금액과_정산일이_포함된다() {
        when(notificationHistoryRepository.existsByServiceNameAndBillingMonth("Netflix", billingMonth))
                .thenReturn(false);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        notificationService.notify(subscription, billingDate);

        verify(telegramClient).sendMessage(captor.capture());
        String message = captor.getValue();
        assertThat(message).contains("Netflix");
        assertThat(message).contains("17,000");
        assertThat(message).contains("2026-04-19");
    }

    // ─── 중복 방지 (이력 있음 + Calendar 있음) ────────────────

    @Test
    void DB_이력이_있고_Calendar_일정도_있으면_아무것도_실행하지_않는다() {
        when(notificationHistoryRepository.existsByServiceNameAndBillingMonth("Netflix", billingMonth))
                .thenReturn(true);
        when(googleCalendarClient.existsEvent(subscription, billingDate))
                .thenReturn(true);

        notificationService.notify(subscription, billingDate);

        verify(googleCalendarClient, never()).createEvent(any(), any());
        verify(telegramClient, never()).sendMessage(anyString());
        verify(notificationHistoryRepository, never()).save(any());
    }

    // ─── Calendar 복구 (이력 있음 + Calendar 없음) ───────────

    @Test
    void DB_이력이_있고_Calendar_일정이_없으면_Calendar만_재생성한다() {
        when(notificationHistoryRepository.existsByServiceNameAndBillingMonth("Netflix", billingMonth))
                .thenReturn(true);
        when(googleCalendarClient.existsEvent(subscription, billingDate))
                .thenReturn(false);

        notificationService.notify(subscription, billingDate);

        verify(googleCalendarClient).createEvent(subscription, billingDate);
        verify(telegramClient, never()).sendMessage(anyString());
        verify(notificationHistoryRepository, never()).save(any());
    }

    // ─── billingMonth 계산 검증 ───────────────────────────────

    @Test
    void billingDate의_월_1일이_billingMonth로_사용된다() {
        LocalDate midMonthBillingDate = LocalDate.of(2026, 4, 19);
        LocalDate expectedBillingMonth = LocalDate.of(2026, 4, 1);

        when(notificationHistoryRepository.existsByServiceNameAndBillingMonth("Netflix", expectedBillingMonth))
                .thenReturn(false);

        notificationService.notify(subscription, midMonthBillingDate);

        verify(notificationHistoryRepository)
                .existsByServiceNameAndBillingMonth("Netflix", expectedBillingMonth);
    }
}
