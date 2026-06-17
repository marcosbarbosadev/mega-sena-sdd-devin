package com.megasena.sync.identidade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByProviderUid(String providerUid);

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByEstado(EstadoConta estado);

    @Modifying
    @Query("UPDATE Usuario u SET u.estado = :novoEstado, u.atualizadoEm = :agora WHERE u.id = :id AND u.estado = 'PENDENTE'")
    int transicionarDePendente(@Param("id") UUID id, @Param("novoEstado") EstadoConta novoEstado, @Param("agora") Instant agora);
}
