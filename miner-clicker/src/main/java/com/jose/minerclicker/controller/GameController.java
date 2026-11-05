package com.jose.minerclicker.controller;

import com.jose.minerclicker.model.GameState;
import com.jose.minerclicker.service.JogadorService;
import com.jose.minerclicker.service.TelegramAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {
    private final JogadorService jogadorService;
    private final TelegramAuthService telegramAuth;

    public GameController(JogadorService jogadorService, TelegramAuthService telegramAuth) {
        this.jogadorService = jogadorService;
        this.telegramAuth = telegramAuth;
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

    @PostMapping("/ads/dobrar")
    public ResponseEntity<GameState> dobrarGanhos(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId) {
        return ResponseEntity.ok(jogadorService.dobrarGanhos(usuario(initData, telegramId, null).id()));
    }

    @PostMapping("/ads/resfriar")
    public ResponseEntity<GameState> resfriarPC(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId) {
        return ResponseEntity.ok(jogadorService.resfriarPC(usuario(initData, telegramId, null).id()));
    }

    @PostMapping("/ads/token-saque")
    public ResponseEntity<GameState> gerarToken(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId) {
        return ResponseEntity.ok(jogadorService.ganharTokenSaque(usuario(initData, telegramId, null).id()));
    }

    @PostMapping("/sacar")
    public ResponseEntity<GameState> sacar(@RequestHeader(value = "X-Telegram-Init-Data", required = false) String initData,
            @RequestParam(required = false) String telegramId, @RequestParam int valorSatoshis) {
        return ResponseEntity.ok(jogadorService.realizarSaque(usuario(initData, telegramId, null).id(), valorSatoshis));
    }
}
