package com.jose.minerclicker.service;

import com.jose.minerclicker.model.RewardedAdSession;
import com.jose.minerclicker.repository.JogadorRepository;
import com.jose.minerclicker.repository.RewardedAdSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class RewardedAdService {
    private static final int DURACAO_SESSAO_MINUTOS = 30;

    private final RewardedAdSessionRepository sessionRepository;
    private final JogadorRepository jogadorRepository;
    private final JogadorService jogadorService;
    private final String zoneId;
    private final String sdkUrl;
    private final String postbackSecret;

    public RewardedAdService(RewardedAdSessionRepository sessionRepository,
                             JogadorRepository jogadorRepository,
                             JogadorService jogadorService,
                             @Value("${app.monetag.zone-id:}") String zoneId,
                             @Value("${app.monetag.sdk-url:}") String sdkUrl,
                             @Value("${app.monetag.postback-secret:}") String postbackSecret) {
        this.sessionRepository = sessionRepository;
        this.jogadorRepository = jogadorRepository;
        this.jogadorService = jogadorService;
        this.zoneId = zoneId == null ? "" : zoneId.trim();
        this.sdkUrl = sdkUrl == null ? "" : sdkUrl.trim();
        this.postbackSecret = postbackSecret == null ? "" : postbackSecret;
    }

    @Transactional(readOnly = true)
    public AdConfig configuracao() {
        return new AdConfig(configurado(), zoneId, sdkUrl);
    }

    public AdSessionView criarSessao(String telegramId, String tipo) {
        exigirConfiguracao();
        if (!jogadorRepository.existsById(telegramId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entre no jogo antes de iniciar um anúncio.");
        }
        RewardedAdSession.RewardType rewardType = converterTipo(tipo);
        LocalDateTime agora = LocalDateTime.now();
        String id = UUID.randomUUID().toString();
        sessionRepository.save(new RewardedAdSession(id, telegramId, rewardType,
                agora, agora.plusMinutes(DURACAO_SESSAO_MINUTOS)));
        return new AdSessionView(id, "PENDING");
    }

    public AdSessionStatus consultarSessao(String telegramId, String sessionId) {
        RewardedAdSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão de anúncio não encontrada."));
        if (!session.getTelegramId().equals(telegramId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Essa sessão de anúncio pertence a outra conta.");
        }
        if (session.getStatus() == RewardedAdSession.RewardStatus.PENDING && LocalDateTime.now().isAfter(session.getExpiresAt())) {
            session.setStatus(RewardedAdSession.RewardStatus.EXPIRED);
        }
        return new AdSessionStatus(session.getStatus().name());
    }

    public void processarPostback(String secret, String ymid, String eventType,
                                  String rewardEventType, String receivedZoneId) {
        if (postbackSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Postback de anúncios ainda não configurado.");
        }
        if (secret == null || !MessageDigest.isEqual(postbackSecret.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Postback inválido.");
        }
        // Cliques não finalizam o anúncio. Somente o postback da impressão pode liberar prêmio.
        if (!"impression".equals(eventType) || ymid == null || !ymid.matches("[0-9a-fA-F-]{36}")) return;
        if (!zoneId.equals(receivedZoneId)) return;

        RewardedAdSession session = sessionRepository.findByIdForUpdate(ymid).orElse(null);
        if (session == null || session.getStatus() != RewardedAdSession.RewardStatus.PENDING) return;
        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            session.setStatus(RewardedAdSession.RewardStatus.EXPIRED);
            return;
        }

        if ("valued".equals(rewardEventType)) {
            switch (session.getRewardType()) {
                case DOUBLE -> jogadorService.dobrarGanhos(session.getTelegramId());
                case COOL -> jogadorService.resfriarPC(session.getTelegramId());
                case WITHDRAWAL_TOKEN -> jogadorService.ganharTokenSaque(session.getTelegramId());
            }
            session.setStatus(RewardedAdSession.RewardStatus.REWARDED);
        } else if ("non_valued".equals(rewardEventType)) {
            session.setStatus(RewardedAdSession.RewardStatus.NON_VALUED);
        }
    }

    private boolean configurado() {
        return zoneId.matches("\\d+") && sdkUrl.startsWith("https://") && !postbackSecret.isBlank();
    }

    private void exigirConfiguracao() {
        if (!configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Anúncios reais ainda não foram configurados no servidor.");
        }
    }

    private RewardedAdSession.RewardType converterTipo(String tipo) {
        if (tipo == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de anúncio inválido.");
        return switch (tipo) {
            case "dobrar" -> RewardedAdSession.RewardType.DOUBLE;
            case "resfriar" -> RewardedAdSession.RewardType.COOL;
            case "saque" -> RewardedAdSession.RewardType.WITHDRAWAL_TOKEN;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de anúncio inválido.");
        };
    }

    public record AdConfig(boolean enabled, String zoneId, String sdkUrl) {}
    public record AdSessionView(String sessionId, String status) {}
    public record AdSessionStatus(String status) {}
}
