package com.billalarmbot;

import com.billalarmbot.client.GoogleCalendarClient;
import com.billalarmbot.client.GoogleSheetsClient;
import com.billalarmbot.client.TelegramClient;
import com.billalarmbot.scheduler.BillAlarmScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BillAlarmBotApplicationTests {

    // 외부 API 클라이언트 mock — 실제 API 호출 차단
    @MockitoBean
    GoogleSheetsClient googleSheetsClient;

    @MockitoBean
    GoogleCalendarClient googleCalendarClient;

    @MockitoBean
    TelegramClient telegramClient;

    // 스케줄러 mock — ApplicationReadyEvent의 recover() 실행 차단
    @MockitoBean
    BillAlarmScheduler billAlarmScheduler;

    @Test
    void contextLoads() {
    }

}
