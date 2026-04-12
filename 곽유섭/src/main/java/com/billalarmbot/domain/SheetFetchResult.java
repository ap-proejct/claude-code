package com.billalarmbot.domain;

import java.util.List;

public record SheetFetchResult(
        List<Subscription> validSubscriptions,
        List<String> invalidRowMessages
) {
    public boolean hasErrors() {
        return !invalidRowMessages.isEmpty();
    }
}
