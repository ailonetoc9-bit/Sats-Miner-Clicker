package com.jose.minerclicker.controller;

import com.jose.minerclicker.model.Jogador;
import com.jose.minerclicker.service.JogadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    private final JogadorService jogadorService;

    public GameController(JogadorService jogadorService) {
        this.jogadorService = jogadorService;
    }

    @PostMapping("/login")
    public ResponseEntity<Jogador> login(@RequestParam String telegramId, @RequestParam String nome) {
        Jogador jogador = jogadorService.obterOuCriarJogador(telegramId, nome);
        // O login já serve como uma sincronização, pois calcula o tempo que ele ficou offline!
        jogador = jogadorService.processarProgresso(telegramId, 0);
        return ResponseEntity.ok(jogador);
    }

    @PostMapping("/sync")
    public ResponseEntity<Jogador> sincronizar(@RequestParam String telegramId, @RequestParam int cliques) {
        Jogador jogadorAtualizado = jogadorService.processarProgresso(telegramId, cliques);
        return ResponseEntity.ok(jogadorAtualizado);
    }

    // Endpoint para comprar itens
    @PostMapping("/comprar")
    public ResponseEntity<Jogador> comprar(
            @RequestParam String telegramId,
            @RequestParam String tipo, // "CLIQUE" ou "PASSIVO"
            @RequestParam long preco,
            @RequestParam int poderAdicional) {

        Jogador jogadorAtualizado = jogadorService.comprarUpgrade(telegramId, tipo, preco, poderAdicional);
        return ResponseEntity.ok(jogadorAtualizado);
    }
    // Assiste anúncio para resfriar a garagem
    @PostMapping("/ads/resfriar")
    public ResponseEntity<Jogador> resfriarPC(@RequestParam String telegramId) {
        Jogador atualizado = jogadorService.resfriarPC(telegramId);
        return ResponseEntity.ok(atualizado);
    }

    // Assiste anúncio para ganhar token de gás
    @PostMapping("/ads/token-saque")
    public ResponseEntity<Jogador> gerarToken(@RequestParam String telegramId) {
        Jogador atualizado = jogadorService.ganharTokenSaque(telegramId);
        return ResponseEntity.ok(atualizado);
    }

    // Efetua o saque
    @PostMapping("/sacar")
    public ResponseEntity<Jogador> sacar(
            @RequestParam String telegramId,
            @RequestParam int valorSatoshis) {

        Jogador atualizado = jogadorService.realizarSaque(telegramId, valorSatoshis);
        return ResponseEntity.ok(atualizado);
    }
}