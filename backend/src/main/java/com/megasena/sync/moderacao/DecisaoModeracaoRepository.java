package com.megasena.sync.moderacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DecisaoModeracaoRepository extends JpaRepository<DecisaoModeracao, Long> {
    List<DecisaoModeracao> findByUsuarioId(UUID usuarioId);
}
