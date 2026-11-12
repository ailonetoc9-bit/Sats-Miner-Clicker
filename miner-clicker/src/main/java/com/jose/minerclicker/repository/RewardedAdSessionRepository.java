package com.jose.minerclicker.repository;

import com.jose.minerclicker.model.RewardedAdSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RewardedAdSessionRepository extends JpaRepository<RewardedAdSession, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @org.springframework.data.jpa.repository.Query("select s from RewardedAdSession s where s.id = :id")
    Optional<RewardedAdSession> findByIdForUpdate(@Param("id") String id);
}
