package com.billalarmbot.client;

import com.billalarmbot.domain.Subscription;

import java.time.LocalDate;

public interface GoogleCalendarClient {

    void createEvent(Subscription subscription, LocalDate billingDate);

    boolean existsEvent(Subscription subscription, LocalDate billingDate);
}
