package com.jose.minerclicker.service;

import com.jose.minerclicker.model.Jogador;
import com.jose.minerclicker.repository.JogadorRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class JogadorService {

    private final JogadorRepository repository;
    private static final int MAX_CLIQUES_POR_SEGUNDO = 10;

    public JogadorService(JogadorRepository repository) {
        this.repository = repository;
    }

    public Jogador obterOuCriarJogador(String telegramId, String nome) {
        return repository.findById(telegramId).orElseGet(() -> {
            Jogador novo = new Jogador(telegramId, nome);
            return repository.save(novo);
        });
    }

    // O coração do jogo: Processa cliques, tempo offline, satoshis e temperatura
    public Jogador processarProgresso(String telegramId, int cliquesEnviados) {
        Jogador jogador = repository.findById(telegramId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        LocalDateTime agora = LocalDateTime.now();
        LocalDate hoje = agora.toLocalDate();
        long segundosPassados = Duration.between(jogador.getUltimaSincronizacao(), agora).getSeconds();
        if (segundosPassados < 1) segundosPassados = 1;

        // 1. Reset Diário do Temporizador
        if (jogador.getUltimoDiaJogado() == null || !jogador.getUltimoDiaJogado().isEqual(hoje)) {
            jogador.setUltimoDiaJogado(hoje);
            jogador.setTempoAtivoHojeSegundos(0);
            jogador.setSatoshisGanhosHoje(0); // Zera o limite para o novo dia
        }

        // 2. Progresso de Satoshis (2 por minuto, máx 120)
        jogador.setTempoAtivoHojeSegundos(jogador.getTempoAtivoHojeSegundos() + segundosPassados);
        int minutosJogadosHoje = (int) (jogador.getTempoAtivoHojeSegundos() / 60);
        int maxSatoshisMerecidos = Math.min(minutosJogadosHoje * 2, 120);

        int satoshisNovos = maxSatoshisMerecidos - jogador.getSatoshisGanhosHoje();
        if (satoshisNovos > 0) {
            jogador.setSatoshisGanhosHoje(jogador.getSatoshisGanhosHoje() + satoshisNovos);
            jogador.setSaldoSatoshisAcumulados(jogador.getSaldoSatoshisAcumulados() + satoshisNovos);
        }

        // 3. Sistema de Temperatura (Penalidade no Passivo)
        long tempoSemResfriar = Duration.between(jogador.getUltimoResfriamento(), agora).getSeconds();
        double multiplicadorPassivo = 1.0;
        if (tempoSemResfriar > 900) { // 15 minutos (900 segundos) sem ver anúncio
            multiplicadorPassivo = 0.1; // PC superaquece e a renda passiva cai para 10%
        }

        // 4. Cálculos Financeiros
        long maxCliquesPermitidos = segundosPassados * MAX_CLIQUES_POR_SEGUNDO;
        int cliquesValidos = cliquesEnviados > maxCliquesPermitidos ? (int) maxCliquesPermitidos : cliquesEnviados;

        long moedasDosCliques = (long) cliquesValidos * jogador.getPoderDeMineracaoPorClique();
        long moedasPassivas = (long) (segundosPassados * jogador.getRendaPassivaPorSegundo() * multiplicadorPassivo);

        jogador.setMoedasVirtuais(jogador.getMoedasVirtuais() + moedasDosCliques + moedasPassivas);
        jogador.setUltimaSincronizacao(agora);

        return repository.save(jogador);
    }

    // Método chamado quando o usuário assiste a um anúncio de resfriamento
    public Jogador resfriarPC(String telegramId) {
        Jogador jogador = repository.findById(telegramId).orElseThrow();
        jogador.setUltimoResfriamento(LocalDateTime.now());
        return repository.save(jogador);
    }

    // Método chamado quando o usuário assiste a um anúncio para o Saque
    public Jogador ganharTokenSaque(String telegramId) {
        Jogador jogador = repository.findById(telegramId).orElseThrow();
        jogador.setTokensDeSaque(jogador.getTokensDeSaque() + 1);
        return repository.save(jogador);
    }

    // A lógica real de Saque
    public Jogador realizarSaque(String telegramId, int valorSats) {
        Jogador jogador = repository.findById(telegramId).orElseThrow();

        if (jogador.getTokensDeSaque() < 1) {
            throw new RuntimeException("Você precisa de um Token de Gás (Assista um anúncio)");
        }
        if (jogador.getSaldoSatoshisAcumulados() < valorSats) {
            throw new RuntimeException("Saldo de Satoshis insuficiente");
        }

        // Desconta o saldo e o token
        jogador.setSaldoSatoshisAcumulados(jogador.getSaldoSatoshisAcumulados() - valorSats);
        jogador.setTokensDeSaque(jogador.getTokensDeSaque() - 1);

        // Aqui no futuro entrará a integração com a API da FaucetPay para enviar o dinheiro real
        System.out.println("Enviando " + valorSats + " satoshis para o jogador " + telegramId);

        return repository.save(jogador);
    }
    // Lógica da Loja
    public Jogador comprarUpgrade(String telegramId, String tipo, long preco, int poderAdicional) {
        Jogador jogador = repository.findById(telegramId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        if (jogador.getMoedasVirtuais() >= preco) {
            // Desconta o valor
            jogador.setMoedasVirtuais(jogador.getMoedasVirtuais() - preco);

            // Aplica o upgrade dependendo do tipo
            if (tipo.equalsIgnoreCase("CLIQUE")) {
                jogador.setPoderDeMineracaoPorClique(jogador.getPoderDeMineracaoPorClique() + poderAdicional);
            } else if (tipo.equalsIgnoreCase("PASSIVO")) {
                jogador.setRendaPassivaPorSegundo(jogador.getRendaPassivaPorSegundo() + poderAdicional);
            }
            return repository.save(jogador);
        } else {
            throw new RuntimeException("Saldo insuficiente");
        }
    }
}