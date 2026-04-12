package com.billalarmbot.scheduler;

import com.billalarmbot.domain.Subscription;
import com.billalarmbot.service.NotificationService;
import com.billalarmbot.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillAlarmScheduler {

    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;

    // 앱 시작 시 D-1 ~ D-7 전체 범위 복구 실행
    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        log.info("[BillAlarmScheduler] 시작 복구 스캔 시작 (D-1 ~ D-7)");
        processTargets(subscriptionService.getSubscriptionsDueWithinSevenDays(), "복구");
    }

    // 매일 정해진 시간에 D-7 정확히 실행
    @Scheduled(cron = "${scheduler.cron}")
    public void run() {
        log.info("[BillAlarmScheduler] 정기 스케줄 실행 (D-7)");
        processTargets(subscriptionService.getSubscriptionsDueInSevenDays(), "정기");
    }

    private void processTargets(List<Subscription> targets, String mode) {
        LocalDate today = LocalDate.now();
        int successCount = 0;

        for (Subscription subscription : targets) {
            try {
                LocalDate billingDate = subscriptionService.nextBillingDate(subscription, today);
                notificationService.notify(subscription, billingDate);
                successCount++;
            } catch (Exception e) {
                log.error("[BillAlarmScheduler] 알림 처리 실패 — 서비스명: {}, 오류: {}",
                        subscription.getServiceName(), e.getMessage(), e);
            }
        }

        log.info("[BillAlarmScheduler] {} 완료 — 처리: {}건", mode, successCount);
    }
}
