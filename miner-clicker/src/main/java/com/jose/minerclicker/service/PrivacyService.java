package com.jose.minerclicker.service;

import com.jose.minerclicker.repository.JogadorRepository;
import com.jose.minerclicker.repository.RewardedAdSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivacyService {
    private final JogadorRepository jogadorRepository;
    private final RewardedAdSessionRepository adSessionRepository;

    public PrivacyService(JogadorRepository jogadorRepository,
                          RewardedAdSessionRepository adSessionRepository) {
        this.jogadorRepository = jogadorRepository;
        this.adSessionRepository = adSessionRepository;
    }

    @Transactional
    public void apagarDadosDoJogador(String telegramId) {
        adSessionRepository.deleteAllByTelegramId(telegramId);
        jogadorRepository.deleteById(telegramId);
    }
}
