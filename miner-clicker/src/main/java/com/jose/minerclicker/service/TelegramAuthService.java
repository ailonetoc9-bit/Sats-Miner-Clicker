package com.jose.minerclicker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@Service
public class TelegramAuthService {
    private final ObjectMapper mapper;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.dev-mode:false}")
    private boolean devMode;

    public TelegramAuthService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Usuario validar(String initData, String idTeste, String nomeTeste) {
        if ((initData == null || initData.isBlank()) && devMode) {
            return new Usuario(idTeste == null || idTeste.isBlank() ? "123456789" : idTeste,
                    nomeTeste == null || nomeTeste.isBlank() ? "Jogador local" : nomeTeste);
        }
        if (botToken == null || botToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Autenticação do Telegram ainda não configurada.");
        }
        try {
            Map<String, String> campos = new TreeMap<>();
            for (String parte : initData.split("&")) {
                String[] chaveValor = parte.split("=", 2);
                campos.put(URLDecoder.decode(chaveValor[0], StandardCharsets.UTF_8),
                        chaveValor.length == 2 ? URLDecoder.decode(chaveValor[1], StandardCharsets.UTF_8) : "");
            }
            String hashRecebido = campos.remove("hash");
            if (hashRecebido == null || !MessageDigest.isEqual(hashRecebido.getBytes(StandardCharsets.UTF_8),
                    hmacHex(chaveSecreta(), String.join("\n", campos.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue()).toList())).getBytes(StandardCharsets.UTF_8))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão do Telegram inválida.");
            }
            long authDate = Long.parseLong(campos.getOrDefault("auth_date", "0"));
            long idade = Instant.now().getEpochSecond() - authDate;
            if (idade < -60 || idade > 86_400) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão do Telegram expirada.");
            JsonNode usuario = mapper.readTree(campos.get("user"));
            String id = usuario.path("id").asText();
            if (id.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário do Telegram ausente.");
            String nome = usuario.path("first_name").asText("Jogador");
            String sobrenome = usuario.path("last_name").asText();
            if (!sobrenome.isBlank()) nome += " " + sobrenome;
            return new Usuario(id, nome);
        } catch (ResponseStatusException erro) {
            throw erro;
        } catch (Exception erro) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível validar a sessão do Telegram.");
        }
    }

    private byte[] chaveSecreta() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacHex(byte[] chave, String mensagem) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(chave, "HmacSHA256"));
        byte[] bytes = mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    public record Usuario(String id, String nome) {}
}
