package com.megasena.sync.identidade;

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
@Table(name = "evento_identidade")
public class EventoIdentidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", columnDefinition = "BINARY(16)")
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoEvento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_login")
    private MetodoLogin metodoLogin;

    @Column(name = "sucesso", nullable = false)
    private boolean sucesso;

    @Column(name = "motivo", length = 80)
    private String motivo;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @PrePersist
    void prePersist() {
        this.criadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public TipoEvento getTipo() { return tipo; }
    public void setTipo(TipoEvento tipo) { this.tipo = tipo; }
    public MetodoLogin getMetodoLogin() { return metodoLogin; }
    public void setMetodoLogin(MetodoLogin metodoLogin) { this.metodoLogin = metodoLogin; }
    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCriadoEm() { return criadoEm; }
}
