package com.jose.minerclicker.controller;

import com.jose.minerclicker.model.GameState;
import com.jose.minerclicker.service.JogadorService;
import com.jose.minerclicker.service.PrivacyService;
import com.jose.minerclicker.service.RewardedAdService;
import com.jose.minerclicker.service.TelegramAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {
    private final JogadorService jogadorService;
    private final TelegramAuthService telegramAuth;
    private final RewardedAdService rewardedAdService;
    private final PrivacyService privacyService;

    public GameController(JogadorService jogadorService, TelegramAuthService telegramAuth,
                          RewardedAdService rewardedAdService, PrivacyService privacyService) {
        this.jogadorService = jogadorService;
        this.telegramAuth = telegramAuth;
        this.rewardedAdService = rewardedAdService;
        this.privacyService = privacyService;
    }

    private TelegramAuthService.Usuario usuario(String initData, String idTeste, String nomeTeste) {
        return telegramAuth.validar(initData, idTeste, nomeTeste);
    }

    @PostMapping("/login")
    public ResponseEntity<GameState> login(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId, @RequestParam(required = false) String nome) {
        var usuario = usuario(initData, telegramId, nome);
        return ResponseEntity.ok(jogadorService.obterOuCriarJogador(usuario.id(), usuario.nome()));
    }

    @PostMapping("/sync")
    public ResponseEntity<GameState> sincronizar(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId, @RequestParam(defaultValue = "0") int cliques) {
        return ResponseEntity.ok(jogadorService.processarProgresso(usuario(initData, telegramId, null).id(), cliques));
    }

    @PostMapping("/comprar")
    public ResponseEntity<GameState> comprarUpgrade(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId) {
        return ResponseEntity.ok(jogadorService.comprarUpgrade(usuario(initData, telegramId, null).id()));
    }

    @PostMapping("/contratados/comprar")
    public ResponseEntity<GameState> comprarContratado(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId, @RequestParam String contratadoId) {
        return ResponseEntity.ok(jogadorService.comprarContratado(usuario(initData, telegramId, null).id(), contratadoId));
    }

    @PostMapping("/tarefas/resgatar")
    public ResponseEntity<GameState> resgatarTarefa(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId, @RequestParam String tarefaId) {
        return ResponseEntity.ok(jogadorService.resgatarTarefa(usuario(initData, telegramId, null).id(), tarefaId));
    }

    @GetMapping("/ads/config")
    public ResponseEntity<RewardedAdService.AdConfig> configuracaoAnuncios() {
        return ResponseEntity.ok(rewardedAdService.configuracao());
    }

    @PostMapping("/ads/sessao")
    public ResponseEntity<RewardedAdService.AdSessionView> criarSessaoAnuncio(
            @RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId, @RequestParam String tipo) {
        var jogador = usuario(initData, telegramId, null);
        // O Mini App pode solicitar o anúncio logo após reabrir sem que a sincronização
        // do perfil tenha terminado. Garante o registro antes de criar a sessão.
        jogadorService.obterOuCriarJogador(jogador.id(), jogador.nome());
        return ResponseEntity.ok(rewardedAdService.criarSessao(jogador.id(), tipo));
    }

    @GetMapping("/ads/sessao/{id}")
    public ResponseEntity<RewardedAdService.AdSessionStatus> consultarSessaoAnuncio(
            @RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId, @PathVariable String id) {
        return ResponseEntity.ok(rewardedAdService.consultarSessao(usuario(initData, telegramId, null).id(), id));
    }

    @GetMapping("/ads/postback")
    public ResponseEntity<Void> receberPostbackAnuncio(
            @RequestParam(required = false) String secret,
            @RequestParam(required = false) String ymid,
            @RequestParam(required = false, name = "event_type") String eventType,
            @RequestParam(required = false, name = "reward_event_type") String rewardEventType,
            @RequestParam(required = false, name = "zone_id") String zoneId) {
        rewardedAdService.processarPostback(secret, ymid, eventType, rewardEventType, zoneId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/privacy/data")
    public ResponseEntity<Void> apagarDados(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId) {
        privacyService.apagarDadosDoJogador(usuario(initData, telegramId, null).id());
        return ResponseEntity.noContent().build();
    }
}
