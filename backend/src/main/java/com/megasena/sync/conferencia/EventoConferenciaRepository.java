package com.megasena.sync.conferencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventoConferenciaRepository extends JpaRepository<EventoConferencia, Long> {

    Optional<EventoConferencia> findByJogoIdAndConcursoNumero(UUID jogoId, Integer concursoNumero);

    List<EventoConferencia> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

    void deleteByJogoId(UUID jogoId);
}
