package com.jose.minerclicker.service;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;

@Service
public class TelegramBotService {
    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);
    private static final String GAME_URL = "https://sats-miner-clicker-production.up.railway.app/";

    private final RestClient http = RestClient.create();
    private final String botToken;
    private final String webhookUrl;
    private final String webhookSecret;

    public TelegramBotService(
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.webhook-url:https://sats-miner-clicker-production.up.railway.app/api/telegram/webhook}") String webhookUrl) {
        this.botToken = botToken;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = botToken.isBlank() ? "" : sha256(botToken);
    }

    public boolean isWebhookSecretValid(String supplied) {
        return supplied != null && !webhookSecret.isBlank()
                && MessageDigest.isEqual(webhookSecret.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }

    public void handleUpdate(JsonNode update) {
        JsonNode message = update.path("message");
        String text = message.path("text").asText("").trim();
        String chatId = message.path("chat").path("id").asText("");
        if (text.isEmpty() || chatId.isEmpty()) return;

        String command = text.split("\\s+", 2)[0].split("@", 2)[0].toLowerCase();
        if (command.equals("/start") || command.equals("/help")) {
            String firstName = message.path("from").path("first_name").asText("").trim();
            sendWelcome(chatId, firstName);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        if (botToken.isBlank() || webhookUrl.isBlank()) return;
        setProfilePhoto();
        setCommands();
        try {
            JsonNode response = http.post()
                    .uri("https://api.telegram.org/bot{token}/setWebhook", botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("url", webhookUrl, "secret_token", webhookSecret, "allowed_updates", List.of("message")))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("ok").asBoolean(false)) {
                log.warn("Telegram webhook was not registered: {}", redactToken(response == null ? "empty response" : response.path("description").asText("unknown error")));
            } else {
                log.info("Telegram bot webhook registered.");
            }
        } catch (Exception error) {
            // Bot delivery should not prevent the game API from starting if Telegram is temporarily unavailable.
            log.warn("Could not register Telegram bot webhook: {}", safeError(error));
        }
    }

    private void setCommands() {
        try {
            JsonNode response = http.post()
                    .uri("https://api.telegram.org/bot{token}/setMyCommands", botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("commands", List.of(
                            Map.of("command", "start", "description", "Abrir o Sats Miner Club"),
                            Map.of("command", "help", "description", "Ver como começar a jogar")
                    )))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("ok").asBoolean(false)) {
                log.warn("Telegram bot commands were not updated: {}", redactToken(response == null ? "empty response" : response.path("description").asText("unknown error")));
            }
        } catch (Exception error) {
            log.warn("Could not update Telegram bot commands: {}", safeError(error));
        }
    }

    private void setProfilePhoto() {
        try (InputStream encodedImage = new ClassPathResource("static/brand/sats-miner-club-logo.png.b64").getInputStream()) {
            byte[] png = Base64.getDecoder().decode(encodedImage.readAllBytes());
            BufferedImage source = ImageIO.read(new java.io.ByteArrayInputStream(png));
            BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgb.createGraphics();
            graphics.setColor(new Color(246, 237, 207));
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(source, 0, 0, null);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(rgb, "jpg", output);

            ByteArrayResource photo = new ByteArrayResource(output.toByteArray()) {
                @Override
                public String getFilename() { return "sats-miner-club.jpg"; }
            };
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("photo", "{\"type\":\"static\",\"photo\":\"attach://profile_photo\"}");
            form.add("profile_photo", photo);
            JsonNode response = http.post()
                    .uri("https://api.telegram.org/bot{token}/setMyProfilePhoto", botToken)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("ok").asBoolean(false)) {
                log.warn("Telegram bot profile photo was not updated: {}", redactToken(response == null ? "empty response" : response.path("description").asText("unknown error")));
            } else {
                log.info("Telegram bot profile photo updated.");
            }
        } catch (Exception error) {
            log.warn("Could not update Telegram bot profile photo: {}", safeError(error));
        }
    }

    private void sendWelcome(String chatId, String firstName) {
        String greeting = firstName.isBlank() ? "Bem-vindo(a)" : "Olá, " + firstName;
        String text = greeting + " ao Sats Miner Club! ⛏️\n\n"
                + "Monte sua garagem de mineração: toque para ganhar moedas, melhore seu setup e contrate profissionais de TI para gerar renda passiva.\n\n"
                + "Complete tarefas para acumular satoshis no jogo. A cada 10 minutos o PC pode superaquecer; resfrie-o para manter a produção. A carteira e os saques ainda são demonstrativos nesta versão.\n\n"
                + "Toque abaixo para abrir o jogo. Boa mineração!";

        Map<String, Object> keyboard = Map.of("inline_keyboard", List.of(List.of(Map.of(
                "text", "⛏️ Jogar Sats Miner Club",
                "web_app", Map.of("url", GAME_URL)
        ))));
        try {
            http.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", text, "reply_markup", keyboard))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception error) {
            log.warn("Could not send Telegram welcome message: {}", safeError(error));
        }
    }

    private String safeError(Exception error) {
        return redactToken(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
    }

    private String redactToken(String value) {
        if (value == null) return "unknown error";
        String redacted = botToken == null || botToken.isBlank() ? value : value.replace(botToken, "[redacted]");
        return redacted.replaceAll("\\b\\d{6,}:[A-Za-z0-9_-]{20,}\\b", "[redacted]");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
