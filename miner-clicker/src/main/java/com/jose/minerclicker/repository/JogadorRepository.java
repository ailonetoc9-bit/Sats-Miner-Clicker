package com.jose.minerclicker.repository;

import com.jose.minerclicker.model.Jogador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JogadorRepository extends JpaRepository<Jogador, String> {
    // O Spring Boot implementa automaticamente os métodos de guardar, procurar e apagar.
}