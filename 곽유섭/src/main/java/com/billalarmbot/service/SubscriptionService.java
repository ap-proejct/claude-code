package com.billalarmbot.service;

import com.billalarmbot.client.GoogleSheetsClient;
import com.billalarmbot.client.TelegramClient;
import com.billalarmbot.domain.SheetFetchResult;
import com.billalarmbot.domain.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final GoogleSheetsClient googleSheetsClient;
    private final TelegramClient telegramClient;

    public List<Subscription> getSubscriptionsDueInSevenDays() {
        return getSubscriptionsDueWithin(7, 7);
    }

    // 재시작 복구용: D-1 ~ D-7 범위 전체 조회
    public List<Subscription> getSubscriptionsDueWithinSevenDays() {
        return getSubscriptionsDueWithin(1, 7);
    }

    private List<Subscription> getSubscriptionsDueWithin(int fromDays, int toDays) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.plusDays(fromDays);
        LocalDate to = today.plusDays(toDays);

        SheetFetchResult result = googleSheetsClient.fetchSubscriptions();

        if (result.hasErrors()) {
            telegramClient.sendMessage(buildErrorMessage(result.invalidRowMessages()));
        }

        List<Subscription> due = result.validSubscriptions().stream()
                .filter(s -> {
                    LocalDate billingDate = nextBillingDate(s, today);
                    return !billingDate.isBefore(from) && !billingDate.isAfter(to);
                })
                .toList();

        log.info("D-{} ~ D-{} 대상 {}건 (범위: {} ~ {})", fromDays, toDays, due.size(), from, to);
        return due;
    }

    public LocalDate nextBillingDate(Subscription subscription, LocalDate today) {
        int billingDay = subscription.getBillingDay();
        LocalDate candidate = resolveDate(today.getYear(), today.getMonthValue(), billingDay);

        if (candidate.isBefore(today)) {
            YearMonth nextMonth = YearMonth.of(today.getYear(), today.getMonthValue()).plusMonths(1);
            candidate = resolveDate(nextMonth.getYear(), nextMonth.getMonthValue(), billingDay);
        }

        return candidate;
    }

    private LocalDate resolveDate(int year, int month, int billingDay) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int actualDay = Math.min(billingDay, yearMonth.lengthOfMonth());
        return LocalDate.of(year, month, actualDay);
    }

    private String buildErrorMessage(List<String> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u26A0\uFE0F <b>Sheets 데이터 오류 감지</b>\n\n");
        sb.append("아래 행의 데이터가 올바르지 않아 처리에서 제외되었습니다:\n\n");
        errors.forEach(error -> sb.append("• ").append(error).append("\n"));
        sb.append("\nGoogle Sheets에서 해당 행을 수정해 주세요.");
        return sb.toString();
    }
}
