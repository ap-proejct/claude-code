package com.billalarmbot.client;

import com.billalarmbot.domain.SheetFetchResult;

public interface GoogleSheetsClient {

    SheetFetchResult fetchSubscriptions();
}
