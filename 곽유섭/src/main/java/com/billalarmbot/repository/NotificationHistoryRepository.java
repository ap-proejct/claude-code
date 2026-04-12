package com.billalarmbot.repository;

import com.billalarmbot.domain.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    boolean existsByServiceNameAndBillingMonth(String serviceName, LocalDate billingMonth);
}
