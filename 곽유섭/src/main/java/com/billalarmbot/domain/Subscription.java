package com.billalarmbot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    private String serviceName;
    private int amount;
    private int billingDay;
    private String paymentMethod;
}
