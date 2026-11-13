package com.jose.minerclicker.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Idempotent schema migration for the persisted anti-abuse fields added to Jogador.
 * Runs after JPA initialization so existing Railway databases are upgraded before
 * the first game request, even when Hibernate schema updates are disabled there.
 */
@Component
public class PlayerSecuritySchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public PlayerSecuritySchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE jogador ADD COLUMN IF NOT EXISTS creditos_de_clique DOUBLE PRECISION NOT NULL DEFAULT 0");
        jdbcTemplate.execute("ALTER TABLE jogador ADD COLUMN IF NOT EXISTS atualizacao_creditos_de_clique TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE jogador ADD COLUMN IF NOT EXISTS ultima_solicitacao_anuncio TIMESTAMP");
    }
}
