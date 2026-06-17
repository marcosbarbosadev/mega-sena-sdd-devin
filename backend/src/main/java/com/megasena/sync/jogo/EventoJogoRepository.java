package com.megasena.sync.jogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventoJogoRepository extends JpaRepository<EventoJogo, Long> {
    void deleteByJogoId(UUID jogoId);
}
