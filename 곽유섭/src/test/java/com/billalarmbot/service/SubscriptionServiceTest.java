package com.billalarmbot.service;

import com.billalarmbot.client.GoogleSheetsClient;
import com.billalarmbot.client.TelegramClient;
import com.billalarmbot.domain.SheetFetchResult;
import com.billalarmbot.domain.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private GoogleSheetsClient googleSheetsClient;

    @Mock
    private TelegramClient telegramClient;

    @InjectMocks
    private SubscriptionService subscriptionService;

    // ─── nextBillingDate 테스트 ───────────────────────────────

    @Test
    void 정산일이_아직_안지난_경우_이번달_날짜를_반환한다() {
        Subscription sub = Subscription.builder().serviceName("Netflix").amount(15000).billingDay(20).paymentMethod("카드").build();
        LocalDate today = LocalDate.of(2026, 4, 10);

        LocalDate result = subscriptionService.nextBillingDate(sub, today);

        assertThat(result).isEqualTo(LocalDate.of(2026, 4, 20));
    }

    @Test
    void 정산일이_이미_지난_경우_다음달로_계산된다() {
        Subscription sub = Subscription.builder().serviceName("Spotify").amount(10000).billingDay(5).paymentMethod("카드").build();
        LocalDate today = LocalDate.of(2026, 4, 10);

        LocalDate result = subscriptionService.nextBillingDate(sub, today);

        assertThat(result).isEqualTo(LocalDate.of(2026, 5, 5));
    }

    @Test
    void 정산일이_오늘과_같으면_오늘_날짜를_반환한다() {
        Subscription sub = Subscription.builder().serviceName("Disney+").amount(13000).billingDay(10).paymentMethod("카드").build();
        LocalDate today = LocalDate.of(2026, 4, 10);

        LocalDate result = subscriptionService.nextBillingDate(sub, today);

        assertThat(result).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    void billingDay가_31일인데_2월인_경우_28일로_처리된다() {
        Subscription sub = Subscription.builder().serviceName("Cloud").amount(30000).billingDay(31).paymentMethod("카드").build();
        LocalDate today = LocalDate.of(2026, 2, 1);

        LocalDate result = subscriptionService.nextBillingDate(sub, today);

        assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void billingDay가_31일인데_4월인_경우_30일로_처리된다() {
        Subscription sub = Subscription.builder().serviceName("Cloud").amount(30000).billingDay(31).paymentMethod("카드").build();
        LocalDate today = LocalDate.of(2026, 4, 1);

        LocalDate result = subscriptionService.nextBillingDate(sub, today);

        assertThat(result).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void 정산일이_12월_말일이고_지난_경우_다음해_1월로_계산된다() {
        Subscription sub = Subscription.builder().serviceName("Annual").amount(100000).billingDay(5).paymentMethod("카드").build();
        LocalDate today = LocalDate.of(2026, 12, 31);

        LocalDate result = subscriptionService.nextBillingDate(sub, today);

        assertThat(result).isEqualTo(LocalDate.of(2027, 1, 5));
    }

    // ─── getSubscriptionsDueInSevenDays 테스트 ────────────────

    @Test
    void getSubscriptionsDueInSevenDays_D7에_해당하는_항목만_반환한다() {
        LocalDate today = LocalDate.now();
        int d7Day = today.plusDays(7).getDayOfMonth();
        int d8Day = today.plusDays(8).getDayOfMonth();

        Subscription dueD7 = Subscription.builder().serviceName("D7서비스").amount(1000).billingDay(d7Day).paymentMethod("카드").build();
        Subscription notDue = Subscription.builder().serviceName("D8서비스").amount(2000).billingDay(d8Day).paymentMethod("카드").build();

        when(googleSheetsClient.fetchSubscriptions())
            .thenReturn(new SheetFetchResult(List.of(dueD7, notDue), Collections.emptyList()));

        List<Subscription> result = subscriptionService.getSubscriptionsDueInSevenDays();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getServiceName()).isEqualTo("D7서비스");
    }

    @Test
    void getSubscriptionsDueInSevenDays_해당_항목_없으면_빈_리스트_반환한다() {
        when(googleSheetsClient.fetchSubscriptions())
            .thenReturn(new SheetFetchResult(Collections.emptyList(), Collections.emptyList()));

        List<Subscription> result = subscriptionService.getSubscriptionsDueInSevenDays();

        assertThat(result).isEmpty();
    }

    // ─── getSubscriptionsDueWithinSevenDays 테스트 ────────────

    @Test
    void getSubscriptionsDueWithinSevenDays_D1부터_D7_범위_항목을_모두_반환한다() {
        LocalDate today = LocalDate.now();
        int d1Day = today.plusDays(1).getDayOfMonth();
        int d7Day = today.plusDays(7).getDayOfMonth();

        Subscription d1Sub = Subscription.builder().serviceName("D1서비스").amount(1000).billingDay(d1Day).paymentMethod("카드").build();
        Subscription d7Sub = Subscription.builder().serviceName("D7서비스").amount(2000).billingDay(d7Day).paymentMethod("카드").build();

        when(googleSheetsClient.fetchSubscriptions())
            .thenReturn(new SheetFetchResult(List.of(d1Sub, d7Sub), Collections.emptyList()));

        List<Subscription> result = subscriptionService.getSubscriptionsDueWithinSevenDays();

        assertThat(result).hasSize(2);
    }

    @Test
    void getSubscriptionsDueWithinSevenDays_오류있는_Sheets_데이터는_Telegram으로_경고발송한다() {
        LocalDate today = LocalDate.now();
        int d7Day = today.plusDays(7).getDayOfMonth();
        Subscription valid = Subscription.builder().serviceName("정상서비스").amount(5000).billingDay(d7Day).paymentMethod("카드").build();

        when(googleSheetsClient.fetchSubscriptions())
            .thenReturn(new SheetFetchResult(List.of(valid), List.of("3행: 금액이 숫자가 아닙니다")));

        subscriptionService.getSubscriptionsDueWithinSevenDays();

        verify(telegramClient).sendMessage(anyString());
    }

    @Test
    void getSubscriptionsDueWithinSevenDays_오류없으면_Telegram_경고를_발송하지_않는다() {
        when(googleSheetsClient.fetchSubscriptions())
            .thenReturn(new SheetFetchResult(Collections.emptyList(), Collections.emptyList()));

        subscriptionService.getSubscriptionsDueWithinSevenDays();

        verify(telegramClient, never()).sendMessage(anyString());
    }
}
