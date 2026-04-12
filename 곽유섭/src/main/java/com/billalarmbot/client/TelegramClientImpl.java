package com.billalarmbot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class TelegramClientImpl implements TelegramClient {

    private static final String API_URL = "https://api.telegram.org/bot%s/sendMessage";

    private final RestTemplate restTemplate;
    private final String botToken;
    private final String chatId;

    public TelegramClientImpl(
            RestTemplate restTemplate,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.chat-id}") String chatId) {
        this.restTemplate = restTemplate;
        this.botToken = botToken;
        this.chatId = chatId;
    }

    @Override
    public void sendMessage(String message) {
        log.info("Sending Telegram message to chatId: {}", chatId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "chat_id", chatId,
                "text", message,
                "parse_mode", "HTML"
        );

        try {
            restTemplate.postForEntity(
                    String.format(API_URL, botToken),
                    new HttpEntity<>(body, headers),
                    String.class
            );
            log.info("Telegram message sent successfully");
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
            throw new RuntimeException("Telegram API call failed", e);
        }
    }
}
