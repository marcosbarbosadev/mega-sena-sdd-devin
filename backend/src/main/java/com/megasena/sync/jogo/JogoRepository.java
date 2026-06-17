package com.megasena.sync.jogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JogoRepository extends JpaRepository<Jogo, UUID> {

    List<Jogo> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

    Optional<Jogo> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
