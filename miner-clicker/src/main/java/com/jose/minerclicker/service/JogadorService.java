package com.jose.minerclicker.service;

import com.jose.minerclicker.model.GameState;
import com.jose.minerclicker.model.Jogador;
import com.jose.minerclicker.repository.JogadorRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JogadorService {

    private final JogadorRepository repository;
    private static final int MAX_SATOSHI_SAQUE_DIA = 100;
    private static final int SATS_LIBERADOS_POR_MINUTO = 2;
    private static final int SEGUNDOS_POR_MINUTO = 60;
    private static final int TEMPO_PARA_SUPERAQUECER_SEGUNDOS = 600;
    private static final int SEGUNDOS_MAX_POR_SYNC_ATIVO = 15;
    private static final int[] MARCOS_CONTRATADOS = {10, 25, 50};
    private static final String[][] UPGRADES = {
            {"Teclado mecânico", "Teclas mais rápidas para minerar", "⌨️"},
            {"Mesa ergonômica", "Mais espaço para seu setup", "🪑"},
            {"Monitor 4K", "Enxergue cada detalhe da mineração", "🖥️"},
            {"Placa de vídeo", "Mais potência para cada toque", "🎮"},
            {"Processador", "Respostas mais rápidas no clique", "🧠"},
            {"Memória RAM", "Mantenha mais processos ativos", "💾"},
            {"SSD NVMe", "Acelere todo o seu setup", "⚡"},
            {"Cadeira gamer", "Conforto para longas sessões", "🪑"}
    };

    private static final List<TaskDefinition> TAREFAS = List.of(
            new TaskDefinition("cliques", "Mãos à obra", "Dê 100 cliques na garagem", "cliques", 100, 2),
            new TaskDefinition("moedas", "Primeiros ganhos", "Ganhe 500 moedas do jogo", "moedas", 500, 2),
            new TaskDefinition("passivo", "Renda trabalhando", "Ganhe 100 moedas com renda passiva", "passivo", 100, 3),
            new TaskDefinition("upgrade_1", "Setup melhorado", "Compre upgrades de setup", "upgrade_1", 1, 3),
            new TaskDefinition("upgrade_5", "Setup profissional", "Compre upgrades de setup", "upgrade_5", 5, 8),
            new TaskDefinition("contratado_1", "Reforce a equipe", "Compre níveis de contratado", "contratado_1", 1, 3),
            new TaskDefinition("contratado_5", "Equipe em expansão", "Compre níveis de contratado", "contratado_5", 5, 6)
    );

    private static final String[] NOMES_CONTRATADOS = {
            "Lia Costa · Suporte técnico", "Caio Nunes · Técnico de redes", "Bia Rocha · QA tester",
            "Ravi Lima · Dev front-end", "Nina Alves · Dev back-end", "Davi Melo · DBA",
            "Luna Reis · DevOps", "Ivo Martins · Segurança da informação",
            "Maya Freitas · Arquiteta de software", "Theo Campos · Engenheiro de IA"
    };

    public JogadorService(JogadorRepository repository) {
        this.repository = repository;
    }

    public GameState obterOuCriarJogador(String telegramId, String nome) {
        Jogador jogador = repository.findById(telegramId).orElseGet(() -> repository.save(new Jogador(telegramId, nome)));
        if (jogador.getUltimaSincronizacao() == null) jogador.setUltimaSincronizacao(LocalDateTime.now());
        processarRendaOffline(jogador, LocalDateTime.now());
        jogador.setUltimaSincronizacao(LocalDateTime.now());
        repository.save(jogador);
        return criarEstado(jogador);
    }

    public GameState processarProgresso(String telegramId, int cliquesEnviados) {
        Jogador jogador = encontrar(telegramId);
        LocalDateTime agora = LocalDateTime.now();
        prepararDia(jogador, agora.toLocalDate());

        long segundosPassados = segundosDesdeUltimaSync(jogador, agora);
        int segundosAtivos = (int) Math.min(segundosPassados, SEGUNDOS_MAX_POR_SYNC_ATIVO);
        long moedasPassivas = calcularRenda(jogador, segundosPassados, agora, true);
        int cliquesValidos = Math.max(0, cliquesEnviados);
        long multiplicador = bonusDobroAtivo(jogador, agora) ? 2L : 1L;
        long moedasDosCliques = (long) cliquesValidos * jogador.getPoderDeMineracaoPorClique() * multiplicador;
        long moedasGanhos = moedasPassivas + moedasDosCliques;

        jogador.setMoedasVirtuais(jogador.getMoedasVirtuais() + moedasGanhos);
        incrementarTarefa(jogador, "cliques", cliquesValidos);
        incrementarTarefa(jogador, "moedas", moedasGanhos);
        incrementarTarefa(jogador, "passivo", moedasPassivas);

        jogador.setTempoAtivoHojeSegundos(jogador.getTempoAtivoHojeSegundos() + segundosAtivos);
        jogador.setSegundosPcAtivoDesdeResfriamento(jogador.getSegundosPcAtivoDesdeResfriamento() + segundosPassados);
        int satsLiberados = Math.min(MAX_SATOSHI_SAQUE_DIA,
                (int) (jogador.getTempoAtivoHojeSegundos() / SEGUNDOS_POR_MINUTO) * SATS_LIBERADOS_POR_MINUTO);
        jogador.setSatoshisLiberadosHoje(satsLiberados);
        // Avança apenas os segundos já contabilizados; preserva a fração restante entre sincronizações.
        jogador.setUltimaSincronizacao(jogador.getUltimaSincronizacao().plusSeconds(segundosPassados));
        return criarEstado(repository.save(jogador));
    }

    public GameState comprarUpgrade(String telegramId) {
        Jogador jogador = encontrar(telegramId);
        sincronizarAntesDaAcao(jogador);
        long preco = custoProximoUpgrade(jogador);
        if (jogador.getMoedasVirtuais() < preco) {
            repository.save(jogador);
            throw new IllegalStateException("Moedas insuficientes para esse upgrade.");
        }
        jogador.setMoedasVirtuais(jogador.getMoedasVirtuais() - preco);
        jogador.setPoderDeMineracaoPorClique(jogador.getPoderDeMineracaoPorClique() + bonusProximoUpgrade(jogador));
        incrementarTarefa(jogador, "upgrade_1", 1);
        incrementarTarefa(jogador, "upgrade_5", 1);
        return criarEstado(repository.save(jogador));
    }

    public GameState comprarContratado(String telegramId, String contratadoId) {
        Jogador jogador = encontrar(telegramId);
        sincronizarAntesDaAcao(jogador);
        int indice = indiceContratado(contratadoId);
        int nivelAtual = jogador.getNiveisContratados().getOrDefault(contratadoId, 0);
        long custo = custoContratado(indice, nivelAtual);
        if (jogador.getMoedasVirtuais() < custo) {
            repository.save(jogador);
            throw new IllegalStateException("Moedas insuficientes para contratar.");
        }
        jogador.setMoedasVirtuais(jogador.getMoedasVirtuais() - custo);
        jogador.getNiveisContratados().put(contratadoId, nivelAtual + 1);
        jogador.setRendaPassivaPorSegundo(rendaTotal(jogador.getNiveisContratados()));
        incrementarTarefa(jogador, "contratado_1", 1);
        incrementarTarefa(jogador, "contratado_5", 1);
        return criarEstado(repository.save(jogador));
    }

    public GameState resgatarTarefa(String telegramId, String tarefaId) {
        Jogador jogador = encontrar(telegramId);
        sincronizarAntesDaAcao(jogador);
        TaskDefinition tarefa = TAREFAS.stream().filter(item -> item.id().equals(tarefaId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada."));
        long progresso = jogador.getProgressoTarefas().getOrDefault(tarefa.chaveProgresso(), 0L);
        long objetivo = objetivoTarefa(tarefa, jogador.getCiclosTarefas().getOrDefault(tarefa.id(), 0));
        if (jogador.getTarefasResgatadas().contains(tarefaId)) throw new IllegalStateException("Essa tarefa já foi resgatada hoje.");
        if (progresso < objetivo) throw new IllegalStateException("Conclua a tarefa antes de resgatar a recompensa.");
        jogador.getTarefasResgatadas().add(tarefaId);
        jogador.getProgressoTarefas().put(tarefa.chaveProgresso(), 0L);
        jogador.setSaldoSatoshisAcumulados(jogador.getSaldoSatoshisAcumulados() + tarefa.recompensa());
        jogador.setSatoshisGanhosHoje(jogador.getSatoshisGanhosHoje() + tarefa.recompensa());
        if (jogador.getTarefasResgatadas().size() == TAREFAS.size()) {
            jogador.getTarefasResgatadas().clear();
            jogador.getProgressoTarefas().clear();
            TAREFAS.forEach(item -> jogador.getCiclosTarefas().merge(item.id(), 1, Integer::sum));
        }
        return criarEstado(repository.save(jogador));
    }

    public GameState dobrarGanhos(String telegramId) {
        Jogador jogador = encontrar(telegramId);
        LocalDateTime agora = LocalDateTime.now();
        jogador.setBonusDobroAte((jogador.getBonusDobroAte() != null && jogador.getBonusDobroAte().isAfter(agora)
                ? jogador.getBonusDobroAte() : agora).plusMinutes(10));
        return criarEstado(repository.save(jogador));
    }

    public GameState resfriarPC(String telegramId) {
        Jogador jogador = encontrar(telegramId);
        LocalDateTime agora = LocalDateTime.now();
        jogador.setUltimoResfriamento(agora);
        jogador.setUltimaSincronizacao(agora);
        jogador.setSegundosPcAtivoDesdeResfriamento(0);
        return criarEstado(repository.save(jogador));
    }

    public GameState ganharTokenSaque(String telegramId) {
        Jogador jogador = encontrar(telegramId);
        jogador.setTokensDeSaque(jogador.getTokensDeSaque() + 1);
        return criarEstado(repository.save(jogador));
    }

    public GameState realizarSaque(String telegramId, int valorSats) {
        Jogador jogador = encontrar(telegramId);
        sincronizarAntesDaAcao(jogador);
        int liberadosAindaHoje = Math.max(0, jogador.getSatoshisLiberadosHoje() - jogador.getSatoshisSacadosHoje());
        long disponiveis = Math.min(jogador.getSaldoSatoshisAcumulados(), liberadosAindaHoje);
        if (valorSats <= 0 || valorSats > disponiveis || valorSats > MAX_SATOSHI_SAQUE_DIA - jogador.getSatoshisSacadosHoje())
            throw new IllegalStateException("Esse valor ainda não está liberado para saque.");
        if (jogador.getTokensDeSaque() < 1) throw new IllegalStateException("Ganhe um token de saque assistindo a um anúncio.");
        jogador.setSaldoSatoshisAcumulados(jogador.getSaldoSatoshisAcumulados() - valorSats);
        jogador.setSatoshisSacadosHoje(jogador.getSatoshisSacadosHoje() + valorSats);
        jogador.setTokensDeSaque(jogador.getTokensDeSaque() - 1);
        // O envio de satoshis permanece fictício nesta primeira versão.
        return criarEstado(repository.save(jogador));
    }

    private void processarRendaOffline(Jogador jogador, LocalDateTime agora) {
        prepararDia(jogador, agora.toLocalDate());
        long segundos = segundosDesdeUltimaSync(jogador, agora);
        long moedasPassivas = calcularRenda(jogador, segundos, agora, false);
        jogador.setMoedasVirtuais(jogador.getMoedasVirtuais() + moedasPassivas);
        incrementarTarefa(jogador, "moedas", moedasPassivas);
        incrementarTarefa(jogador, "passivo", moedasPassivas);
    }

    private void sincronizarAntesDaAcao(Jogador jogador) {
        LocalDateTime agora = LocalDateTime.now();
        prepararDia(jogador, agora.toLocalDate());
        long segundos = Math.min(segundosDesdeUltimaSync(jogador, agora), SEGUNDOS_MAX_POR_SYNC_ATIVO);
        long moedasPassivas = calcularRenda(jogador, segundos, agora, true);
        jogador.setMoedasVirtuais(jogador.getMoedasVirtuais() + moedasPassivas);
        jogador.setTempoAtivoHojeSegundos(jogador.getTempoAtivoHojeSegundos() + segundos);
        jogador.setSegundosPcAtivoDesdeResfriamento(jogador.getSegundosPcAtivoDesdeResfriamento() + segundos);
        jogador.setSatoshisLiberadosHoje(Math.min(MAX_SATOSHI_SAQUE_DIA,
                (int) (jogador.getTempoAtivoHojeSegundos() / SEGUNDOS_POR_MINUTO) * SATS_LIBERADOS_POR_MINUTO));
        incrementarTarefa(jogador, "moedas", moedasPassivas);
        incrementarTarefa(jogador, "passivo", moedasPassivas);
        // Tentativas repetidas de compra não podem apagar frações de segundo da renda passiva.
        jogador.setUltimaSincronizacao(jogador.getUltimaSincronizacao().plusSeconds(segundos));
    }

    private long calcularRenda(Jogador jogador, long segundos, LocalDateTime agora, boolean considerarCalor) {
        if (segundos <= 0 || jogador.getRendaPassivaPorSegundo() <= 0) return 0;
        LocalDateTime inicio = jogador.getUltimaSincronizacao();
        if (inicio == null) return 0;
        List<LocalDateTime> pontos = new ArrayList<>(List.of(inicio, agora));
        long segundosAteCalor = Math.max(0, TEMPO_PARA_SUPERAQUECER_SEGUNDOS - jogador.getSegundosPcAtivoDesdeResfriamento());
        LocalDateTime inicioCalor = considerarCalor ? inicio.plusSeconds(segundosAteCalor) : null;
        adicionarPontoInterno(pontos, inicioCalor, inicio, agora);
        adicionarPontoInterno(pontos, jogador.getBonusDobroAte(), inicio, agora);
        pontos.sort(LocalDateTime::compareTo);

        double segundosPonderados = 0;
        for (int i = 0; i < pontos.size() - 1; i++) {
            LocalDateTime trechoInicio = pontos.get(i);
            LocalDateTime trechoFim = pontos.get(i + 1);
            long duracao = Duration.between(trechoInicio, trechoFim).getSeconds();
            if (duracao <= 0) continue;
            LocalDateTime meio = trechoInicio.plusSeconds(duracao / 2);
            double fator = inicioCalor != null && !meio.isBefore(inicioCalor) ? 0.1 : 1.0;
            if (jogador.getBonusDobroAte() != null && meio.isBefore(jogador.getBonusDobroAte())) fator *= 2.0;
            segundosPonderados += duracao * fator;
        }
        double totalFracionario = segundosPonderados * jogador.getRendaPassivaPorSegundo()
                + jogador.getFracaoMoedasPassivas();
        long moedasInteiras = (long) Math.floor(totalFracionario + 1e-9);
        jogador.setFracaoMoedasPassivas(totalFracionario - moedasInteiras);
        return moedasInteiras;
    }

    private void adicionarPontoInterno(List<LocalDateTime> pontos, LocalDateTime ponto,
                                       LocalDateTime inicio, LocalDateTime fim) {
        if (ponto != null && ponto.isAfter(inicio) && ponto.isBefore(fim)) pontos.add(ponto);
    }

    private void prepararDia(Jogador jogador, LocalDate hoje) {
        if (jogador.getUltimoDiaJogado() == null || !jogador.getUltimoDiaJogado().isEqual(hoje)) {
            jogador.setUltimoDiaJogado(hoje);
            jogador.setTempoAtivoHojeSegundos(0);
            jogador.setSatoshisGanhosHoje(0);
            jogador.setSatoshisLiberadosHoje(0);
            jogador.setSatoshisSacadosHoje(0);
            jogador.getProgressoTarefas().clear();
            jogador.getTarefasResgatadas().clear();
        }
    }

    private long segundosDesdeUltimaSync(Jogador jogador, LocalDateTime agora) {
        if (jogador.getUltimaSincronizacao() == null) return 0;
        return Math.max(0, Duration.between(jogador.getUltimaSincronizacao(), agora).getSeconds());
    }

    private long custoProximoUpgrade(Jogador jogador) {
        int comprados = Math.max(0, jogador.getPoderDeMineracaoPorClique() - 1);
        double multiplicador = Math.pow(1.55, comprados);
        if (multiplicador >= Long.MAX_VALUE / 100.0) return Long.MAX_VALUE;
        return Math.max(100, Math.round(100 * multiplicador));
    }

    private int bonusProximoUpgrade(Jogador jogador) {
        int comprados = Math.max(0, jogador.getPoderDeMineracaoPorClique() - 1);
        return 1 + comprados / 3;
    }

    private String[] proximoUpgrade(Jogador jogador) {
        int comprados = Math.max(0, jogador.getPoderDeMineracaoPorClique() - 1);
        return UPGRADES[comprados % UPGRADES.length];
    }

    private int indiceContratado(String id) {
        if (id == null || !id.matches("c(10|[1-9])")) throw new IllegalArgumentException("Contratado inválido.");
        return Integer.parseInt(id.substring(1)) - 1;
    }

    private long custoContratado(int indice, int nivel) {
        return Math.max(1, Math.round(50.0 * (indice + 1) * Math.pow(1.15, nivel)));
    }

    private long rendaContratado(int indice, int nivel) {
        if (nivel <= 0) return 0;
        long base = (long) (indice + 1) * nivel;
        for (int marco : MARCOS_CONTRATADOS) if (nivel >= marco) base *= 2;
        return base;
    }

    private long rendaTotal(Map<String, Integer> niveis) {
        long total = 0;
        for (int i = 0; i < 10; i++) total += rendaContratado(i, niveis.getOrDefault("c" + (i + 1), 0));
        return total;
    }

    private boolean bonusDobroAtivo(Jogador jogador, LocalDateTime agora) {
        return jogador.getBonusDobroAte() != null && jogador.getBonusDobroAte().isAfter(agora);
    }

    private boolean pcAquecido(Jogador jogador, LocalDateTime agora) {
        return jogador.getSegundosPcAtivoDesdeResfriamento() >= TEMPO_PARA_SUPERAQUECER_SEGUNDOS;
    }

    private void incrementarTarefa(Jogador jogador, String id, long quantidade) {
        if (quantidade > 0) jogador.getProgressoTarefas().merge(id, quantidade, Long::sum);
    }

    private long objetivoTarefa(TaskDefinition tarefa, int ciclo) {
        if (ciclo == 0) return tarefa.objetivo();
        long base = tarefa.objetivo();
        long variacao = Math.floorMod((long) ciclo * 37 + tarefa.id().hashCode(), Math.max(3, base / 3));
        double crescimento = Math.pow(1.28, ciclo);
        long escalado = crescimento >= Long.MAX_VALUE / (double) base ? Long.MAX_VALUE : Math.round(base * crescimento);
        long valor = escalado > Long.MAX_VALUE - variacao ? Long.MAX_VALUE : escalado + variacao;
        if (valor >= Long.MAX_VALUE - 9) return Long.MAX_VALUE / 10 * 10;
        return ((valor + 9) / 10) * 10;
    }

    private Jogador encontrar(String telegramId) {
        return repository.findById(telegramId).orElseThrow(() -> new IllegalStateException("Jogador não encontrado."));
    }

    private GameState criarEstado(Jogador jogador) {
        LocalDateTime agora = LocalDateTime.now();
        List<GameState.TarefaView> tarefas = new ArrayList<>();
        for (TaskDefinition tarefa : TAREFAS) {
            long progresso = jogador.getProgressoTarefas().getOrDefault(tarefa.chaveProgresso(), 0L);
            long objetivo = objetivoTarefa(tarefa, jogador.getCiclosTarefas().getOrDefault(tarefa.id(), 0));
            boolean resgatada = jogador.getTarefasResgatadas().contains(tarefa.id());
            String descricao = tarefa.descricao().replaceFirst("\\d+", Long.toString(objetivo));
            tarefas.add(new GameState.TarefaView(tarefa.id(), tarefa.nome(), descricao,
                    Math.min(progresso, objetivo), objetivo, tarefa.recompensa(),
                    resgatada, progresso >= objetivo && !resgatada));
        }
        List<GameState.ContratadoView> contratados = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String id = "c" + (i + 1);
            int nivel = jogador.getNiveisContratados().getOrDefault(id, 0);
            int proximoMarco = java.util.Arrays.stream(MARCOS_CONTRATADOS).filter(marco -> marco > nivel).findFirst().orElse(0);
            contratados.add(new GameState.ContratadoView(id, NOMES_CONTRATADOS[i], nivel,
                    custoContratado(i, nivel), rendaContratado(i, nivel), rendaContratado(i, nivel + 1), proximoMarco));
        }
        String[] upgrade = proximoUpgrade(jogador);
        return new GameState(jogador.getMoedasVirtuais(), jogador.getSatoshisGanhosHoje(),
                jogador.getSaldoSatoshisAcumulados(), jogador.getSatoshisLiberadosHoje(),
                jogador.getSatoshisSacadosHoje(), jogador.getTokensDeSaque(),
                jogador.getPoderDeMineracaoPorClique(), jogador.getRendaPassivaPorSegundo(),
                jogador.getNivelGaragem(), bonusDobroAtivo(jogador, agora), pcAquecido(jogador, agora),
                custoProximoUpgrade(jogador), upgrade[0], upgrade[1], bonusProximoUpgrade(jogador), upgrade[2], tarefas, contratados);
    }

    private record TaskDefinition(String id, String nome, String descricao, String chaveProgresso,
                                  long objetivo, int recompensa) {}
}
