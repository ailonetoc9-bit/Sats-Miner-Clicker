package com.jose.minerclicker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Column;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
public class Jogador {

    @Id
    private String telegramId;
    private String nome;

    private long moedasVirtuais;

    private int nivelGaragem;
    private int poderDeMineracaoPorClique;
    private long rendaPassivaPorSegundo;
    private double fracaoMoedasPassivas;

    private LocalDateTime ultimaSincronizacao;
    private double creditosDeClique;
    private LocalDateTime atualizacaoCreditosDeClique;
    private LocalDateTime ultimaSolicitacaoAnuncio;

    // O nome da coluna mantém compatibilidade com os saldos já gravados antes da beta de pontos.
    @Column(name = "saldo_satoshis_acumulados")
    private long pontosBeta;
    private LocalDate ultimoDiaJogado;

    // Variáveis de Monetização (Anúncios)
    private LocalDateTime ultimoResfriamento;
    private LocalDateTime bonusDobroAte;
    private long segundosPcAtivoDesdeResfriamento;

    @ElementCollection
    @CollectionTable(name = "jogador_contratados", joinColumns = @JoinColumn(name = "telegram_id"))
    @MapKeyColumn(name = "contratado_id")
    @Column(name = "nivel")
    private Map<String, Integer> niveisContratados = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "jogador_tarefas", joinColumns = @JoinColumn(name = "telegram_id"))
    @MapKeyColumn(name = "tarefa_id")
    @Column(name = "progresso")
    private Map<String, Long> progressoTarefas = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "jogador_tarefas_resgatadas", joinColumns = @JoinColumn(name = "telegram_id"))
    @Column(name = "tarefa_id")
    private Set<String> tarefasResgatadas = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "jogador_ciclos_tarefas", joinColumns = @JoinColumn(name = "telegram_id"))
    @MapKeyColumn(name = "tarefa_id")
    @Column(name = "ciclo")
    private Map<String, Integer> ciclosTarefas = new HashMap<>();

    // Construtor vazio obrigatório para o JPA
    public Jogador() {}

    // Construtor para inicializar um novo jogador
    public Jogador(String telegramId, String nome) {
        this.telegramId = telegramId;
        this.nome = nome;
        this.nivelGaragem = 1;
        this.poderDeMineracaoPorClique = 1;
        this.rendaPassivaPorSegundo = 0;
        this.fracaoMoedasPassivas = 0;
        this.moedasVirtuais = 0;
        this.ultimaSincronizacao = LocalDateTime.now();
        this.pontosBeta = 0;
        this.ultimoDiaJogado = LocalDate.now();
        this.ultimoResfriamento = LocalDateTime.now();
        this.bonusDobroAte = null;
        this.segundosPcAtivoDesdeResfriamento = 0;
    }

    // Getters e Setters
    public String getTelegramId() { return telegramId; }
    public void setTelegramId(String telegramId) { this.telegramId = telegramId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public long getMoedasVirtuais() { return moedasVirtuais; }
    public void setMoedasVirtuais(long moedasVirtuais) { this.moedasVirtuais = moedasVirtuais; }

    public int getNivelGaragem() { return nivelGaragem; }
    public void setNivelGaragem(int nivelGaragem) { this.nivelGaragem = nivelGaragem; }

    public int getPoderDeMineracaoPorClique() { return poderDeMineracaoPorClique; }
    public void setPoderDeMineracaoPorClique(int poderDeMineracaoPorClique) { this.poderDeMineracaoPorClique = poderDeMineracaoPorClique; }

    public long getRendaPassivaPorSegundo() { return rendaPassivaPorSegundo; }
    public void setRendaPassivaPorSegundo(long rendaPassivaPorSegundo) { this.rendaPassivaPorSegundo = rendaPassivaPorSegundo; }

    public double getFracaoMoedasPassivas() { return fracaoMoedasPassivas; }
    public void setFracaoMoedasPassivas(double fracaoMoedasPassivas) { this.fracaoMoedasPassivas = fracaoMoedasPassivas; }

    public LocalDateTime getUltimaSincronizacao() { return ultimaSincronizacao; }
    public void setUltimaSincronizacao(LocalDateTime ultimaSincronizacao) { this.ultimaSincronizacao = ultimaSincronizacao; }

    public double getCreditosDeClique() { return creditosDeClique; }
    public void setCreditosDeClique(double valor) { this.creditosDeClique = Math.max(0, valor); }
    public LocalDateTime getAtualizacaoCreditosDeClique() { return atualizacaoCreditosDeClique; }
    public void setAtualizacaoCreditosDeClique(LocalDateTime valor) { this.atualizacaoCreditosDeClique = valor; }
    public LocalDateTime getUltimaSolicitacaoAnuncio() { return ultimaSolicitacaoAnuncio; }
    public void setUltimaSolicitacaoAnuncio(LocalDateTime valor) { this.ultimaSolicitacaoAnuncio = valor; }

    public long getPontosBeta() { return pontosBeta; }
    public void setPontosBeta(long pontosBeta) { this.pontosBeta = Math.max(0, pontosBeta); }

    public LocalDate getUltimoDiaJogado() { return ultimoDiaJogado; }
    public void setUltimoDiaJogado(LocalDate ultimoDiaJogado) { this.ultimoDiaJogado = ultimoDiaJogado; }

    public LocalDateTime getUltimoResfriamento() { return ultimoResfriamento; }
    public void setUltimoResfriamento(LocalDateTime ultimoResfriamento) { this.ultimoResfriamento = ultimoResfriamento; }
    public long getSegundosPcAtivoDesdeResfriamento() { return segundosPcAtivoDesdeResfriamento; }
    public void setSegundosPcAtivoDesdeResfriamento(long segundos) { this.segundosPcAtivoDesdeResfriamento = Math.max(0, segundos); }

    public LocalDateTime getBonusDobroAte() { return bonusDobroAte; }
    public void setBonusDobroAte(LocalDateTime valor) { this.bonusDobroAte = valor; }

    public Map<String, Integer> getNiveisContratados() { return niveisContratados; }
    public void setNiveisContratados(Map<String, Integer> valor) { this.niveisContratados = valor == null ? new HashMap<>() : valor; }

    public Map<String, Long> getProgressoTarefas() { return progressoTarefas; }
    public void setProgressoTarefas(Map<String, Long> valor) { this.progressoTarefas = valor == null ? new HashMap<>() : valor; }

    public Set<String> getTarefasResgatadas() { return tarefasResgatadas; }
    public void setTarefasResgatadas(Set<String> valor) { this.tarefasResgatadas = valor == null ? new HashSet<>() : valor; }
    public Map<String, Integer> getCiclosTarefas() { return ciclosTarefas; }
    public void setCiclosTarefas(Map<String, Integer> valor) { this.ciclosTarefas = valor == null ? new HashMap<>() : valor; }
}
