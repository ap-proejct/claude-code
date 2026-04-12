package com.billalarmbot.client;

import com.billalarmbot.domain.SheetFetchResult;
import com.billalarmbot.domain.Subscription;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleSheetsClientImpl implements GoogleSheetsClient {

    private static final String RANGE = "A:D";

    private final Sheets sheetsService;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    @Override
    public SheetFetchResult fetchSubscriptions() {
        log.info("Fetching subscriptions from spreadsheet: {}", spreadsheetId);
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, RANGE)
                    .execute();

            List<List<Object>> rows = response.getValues();
            if (rows == null || rows.isEmpty()) {
                log.warn("No data found in spreadsheet");
                return new SheetFetchResult(Collections.emptyList(), Collections.emptyList());
            }

            List<Subscription> validSubscriptions = new ArrayList<>();
            List<String> invalidRowMessages = new ArrayList<>();

            // 1행은 헤더이므로 2행(index 1)부터 처리, 행 번호는 사용자 기준(2부터 시작)
            for (int i = 1; i < rows.size(); i++) {
                int rowNumber = i + 1;
                List<Object> row = rows.get(i);
                List<String> rowErrors = validateRow(row, rowNumber);

                if (rowErrors.isEmpty()) {
                    validSubscriptions.add(mapRowToSubscription(row));
                } else {
                    rowErrors.forEach(error -> {
                        log.warn(error);
                        invalidRowMessages.add(error);
                    });
                }
            }

            log.info("Fetched {}건 유효, {}건 오류", validSubscriptions.size(), invalidRowMessages.size());
            return new SheetFetchResult(validSubscriptions, invalidRowMessages);

        } catch (IOException e) {
            log.error("Failed to fetch subscriptions from Google Sheets", e);
            throw new RuntimeException("Google Sheets API call failed", e);
        }
    }

    private List<String> validateRow(List<Object> row, int rowNumber) {
        List<String> errors = new ArrayList<>();

        if (row.size() < 4) {
            errors.add(String.format("%d행: 열이 부족합니다 (4개 필요, %d개 존재)", rowNumber, row.size()));
            return errors;
        }

        String serviceName = String.valueOf(row.get(0)).trim();
        String amountStr   = String.valueOf(row.get(1)).trim();
        String billingDayStr = String.valueOf(row.get(2)).trim();
        String paymentMethod = String.valueOf(row.get(3)).trim();

        if (serviceName.isBlank()) {
            errors.add(String.format("%d행: 서비스명이 비어 있습니다", rowNumber));
        }
        if (!isValidInteger(amountStr)) {
            errors.add(String.format("%d행: 금액이 숫자가 아닙니다 ('%s')", rowNumber, amountStr));
        }
        if (!isValidBillingDay(billingDayStr)) {
            errors.add(String.format("%d행: 정산일이 1~31 사이의 숫자가 아닙니다 ('%s')", rowNumber, billingDayStr));
        }
        if (paymentMethod.isBlank()) {
            errors.add(String.format("%d행: 결제수단이 비어 있습니다", rowNumber));
        }

        return errors;
    }

    private boolean isValidInteger(String value) {
        try {
            Integer.parseInt(value.replaceAll("[^0-9]", ""));
            return !value.replaceAll("[^0-9]", "").isBlank();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidBillingDay(String value) {
        try {
            int day = Integer.parseInt(value.trim());
            return day >= 1 && day <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Subscription mapRowToSubscription(List<Object> row) {
        return Subscription.builder()
                .serviceName(String.valueOf(row.get(0)).trim())
                .amount(Integer.parseInt(String.valueOf(row.get(1)).trim().replaceAll("[^0-9]", "")))
                .billingDay(Integer.parseInt(String.valueOf(row.get(2)).trim()))
                .paymentMethod(String.valueOf(row.get(3)).trim())
                .build();
    }
}
