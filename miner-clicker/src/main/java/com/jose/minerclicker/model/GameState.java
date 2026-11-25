package com.jose.minerclicker.model;

import java.util.List;

public record GameState(
        long moedasVirtuais,
        long pontosBeta,
        int poderDeMineracaoPorClique,
        long rendaPassivaPorSegundo,
        int nivelGaragem,
        boolean bonusDobroAtivo,
        boolean pcAquecido,
        long custoProximoUpgrade,
        String nomeProximoUpgrade,
        String descricaoProximoUpgrade,
        int bonusProximoUpgrade,
        String iconeProximoUpgrade,
        List<TarefaView> tarefas,
        List<ContratadoView> contratados) {

    public record TarefaView(String id, String nome, String descricao, long progresso, long objetivo,
                             int recompensaPontos, boolean resgatada, boolean pronta) {}

    public record ContratadoView(String id, String nome, int nivel, long custo, long rendaPorSegundo,
                                 long rendaProximoNivel, int proximoMarco) {}
}
