package com.megasena.sync.conferencia;

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
@Table(name = "evento_conferencia")
public class EventoConferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jogo_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID jogoId;

    @Column(name = "concurso_numero", nullable = false)
    private Integer concursoNumero;

    @Column(name = "usuario_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID usuarioId;

    @Column(name = "acertos", nullable = false)
    private int acertos;

    @Enumerated(EnumType.STRING)
    @Column(name = "faixa", nullable = false)
    private Faixa faixa;

    @Column(name = "premiado", nullable = false)
    private boolean premiado;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @PrePersist
    void prePersist() {
        this.criadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getJogoId() { return jogoId; }
    public void setJogoId(UUID jogoId) { this.jogoId = jogoId; }
    public Integer getConcursoNumero() { return concursoNumero; }
    public void setConcursoNumero(Integer concursoNumero) { this.concursoNumero = concursoNumero; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public int getAcertos() { return acertos; }
    public void setAcertos(int acertos) { this.acertos = acertos; }
    public Faixa getFaixa() { return faixa; }
    public void setFaixa(Faixa faixa) { this.faixa = faixa; }
    public boolean isPremiado() { return premiado; }
    public void setPremiado(boolean premiado) { this.premiado = premiado; }
    public Instant getCriadoEm() { return criadoEm; }
}
