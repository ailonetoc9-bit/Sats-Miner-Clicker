package com.jose.minerclicker.repository;

import com.jose.minerclicker.model.Jogador;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JogadorRepository extends JpaRepository<Jogador, String> {
    // O Spring Boot implementa automaticamente os métodos de guardar, procurar e apagar.

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Jogador j where j.telegramId = :telegramId")
    Optional<Jogador> findByIdForUpdate(@Param("telegramId") String telegramId);
}
