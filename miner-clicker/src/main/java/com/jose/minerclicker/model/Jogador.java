package com.jose.minerclicker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Jogador {

    @Id
    private String telegramId;
    private String nome;

    private long moedasVirtuais;
    private int satoshisDiariosGanhos;

    private int nivelGaragem;
    private int poderDeMineracaoPorClique;

    private long rendaPassivaPorSegundo;

    private LocalDateTime ultimaSincronizacao;

    // Variáveis de controle de Satoshis e Tempo
    private int saldoSatoshisAcumulados; // Total que ele tem guardado (pode ser > 120)
    private int satoshisLiberadosHoje; // O que ele já desbloqueou hoje (máx 120)
    private long tempoAtivoHojeSegundos; // Conta os segundos jogados no dia atual
    private LocalDate ultimoDiaJogado; // Para resetar a meia-noite

    // Variáveis de controle de Satoshis e Tempo
    private int saldoSatoshisAcumulados;
    private int satoshisGanhosHoje; // Limite de 120
    private long tempoAtivoHojeSegundos;
    private LocalDate ultimoDiaJogado;

    // Variáveis de Monetização (Anúncios)
    private LocalDateTime ultimoResfriamento; // Controla a temperatura do PC
    private int tokensDeSaque;

    // Construtor vazio obrigatório para o JPA
    public Jogador() {}

    // Construtor para inicializar um novo jogador
    public Jogador(String telegramId, String nome) {
        this.telegramId = telegramId;
        this.nome = nome;
        this.nivelGaragem = 1; // Começa no nível 1
        this.poderDeMineracaoPorClique = 1; // 1 moeda por clique inicial
        this.moedasVirtuais = 0;
        this.satoshisDiariosGanhos = 0;
        this.ultimaSincronizacao = LocalDateTime.now();
        this.rendaPassivaPorSegundo = 0;
        this.saldoSatoshisAcumulados = 0;
        this.satoshisGanhosHoje = 0;
        this.tempoAtivoHojeSegundos = 0;
        this.ultimoDiaJogado = LocalDate.now();
        this.ultimoResfriamento = LocalDateTime.now();
        this.tokensDeSaque = 0;
    }

    public String getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(String telegramId) {
        this.telegramId = telegramId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public long getMoedasVirtuais() {
        return moedasVirtuais;
    }

    public void setMoedasVirtuais(long moedasVirtuais) {
        this.moedasVirtuais = moedasVirtuais;
    }

    public int getSatoshisDiariosGanhos() {
        return satoshisDiariosGanhos;
    }

    public void setSatoshisDiariosGanhos(int satoshisDiariosGanhos) {
        this.satoshisDiariosGanhos = satoshisDiariosGanhos;
    }

    public int getNivelGaragem() {
        return nivelGaragem;
    }

    public void setNivelGaragem(int nivelGaragem) {
        this.nivelGaragem = nivelGaragem;
    }

    public int getPoderDeMineracaoPorClique() {
        return poderDeMineracaoPorClique;
    }

    public void setPoderDeMineracaoPorClique(int poderDeMineracaoPorClique) {
        this.poderDeMineracaoPorClique = poderDeMineracaoPorClique;
    }

    public LocalDateTime getUltimaSincronizacao() {
        return ultimaSincronizacao;
    }

    public void setUltimaSincronizacao(LocalDateTime ultimaSincronizacao) {
        this.ultimaSincronizacao = ultimaSincronizacao;
    }

    public long getRendaPassivaPorSegundo() {
        return rendaPassivaPorSegundo;
    }

    public void setRendaPassivaPorSegundo(long rendaPassivaPorSegundo) {
        this.rendaPassivaPorSegundo = rendaPassivaPorSegundo;
    }

    public int getSaldoSatoshisAcumulados() {
        return saldoSatoshisAcumulados;
    }

    public void setSaldoSatoshisAcumulados(int saldoSatoshisAcumulados) {
        this.saldoSatoshisAcumulados = saldoSatoshisAcumulados;
    }

    public int getSatoshisLiberadosHoje() {
        return satoshisLiberadosHoje;
    }

    public void setSatoshisLiberadosHoje(int satoshisLiberadosHoje) {
        this.satoshisLiberadosHoje = satoshisLiberadosHoje;
    }

    public long getTempoAtivoHojeSegundos() {
        return tempoAtivoHojeSegundos;
    }

    public void setTempoAtivoHojeSegundos(long tempoAtivoHojeSegundos) {
        this.tempoAtivoHojeSegundos = tempoAtivoHojeSegundos;
    }

    public LocalDate getUltimoDiaJogado() {
        return ultimoDiaJogado;
    }

    public void setUltimoDiaJogado(LocalDate ultimoDiaJogado) {
        this.ultimoDiaJogado = ultimoDiaJogado;
    }

    public int getSatoshisGanhosHoje() {
        return satoshisGanhosHoje;
    }

    public void setSatoshisGanhosHoje(int satoshisGanhosHoje) {
        this.satoshisGanhosHoje = satoshisGanhosHoje;
    }

    public LocalDateTime getUltimoResfriamento() {
        return ultimoResfriamento;
    }

    public void setUltimoResfriamento(LocalDateTime ultimoResfriamento) {
        this.ultimoResfriamento = ultimoResfriamento;
    }

    public int getTokensDeSaque() {
        return tokensDeSaque;
    }

    public void setTokensDeSaque(int tokensDeSaque) {
        this.tokensDeSaque = tokensDeSaque;
    }

    // Nota: Prima "Alt + Insert" (ou botão direito -> Generate) no IntelliJ
    // e selecione "Getter and Setter" para gerar os métodos de todas as variáveis acima.
}