package com.jose.minerclicker.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.jose.minerclicker.service.TelegramBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telegram")
public class TelegramBotController {
    private final TelegramBotService botService;

    public TelegramBotController(TelegramBotService botService) {
        this.botService = botService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody JsonNode update) {
        if (!botService.isWebhookSecretValid(secret)) return ResponseEntity.status(401).build();
        botService.handleUpdate(update);
        return ResponseEntity.ok().build();
    }
}
