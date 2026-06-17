package com.megasena.sync.jogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evento_jogo")
public class EventoJogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jogo_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID jogoId;

    @Column(name = "usuario_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoEventoJogo tipo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @PrePersist
    void prePersist() {
        this.criadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getJogoId() { return jogoId; }
    public void setJogoId(UUID jogoId) { this.jogoId = jogoId; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public TipoEventoJogo getTipo() { return tipo; }
    public void setTipo(TipoEventoJogo tipo) { this.tipo = tipo; }
    public Instant getCriadoEm() { return criadoEm; }
}
