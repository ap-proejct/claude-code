package com.billalarmbot.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private int billingDay;

    // 어느 달의 알림인지 (중복 방지 기준)
    @Column(nullable = false)
    private LocalDate billingMonth; // 해당 월의 1일 기준으로 저장 (ex: 2026-04-01)

    @Column(nullable = false)
    private LocalDateTime notifiedAt;
}
