package com.megasena.sync.moderacao;

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
@Table(name = "decisao_moderacao")
public class DecisaoModeracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID usuarioId;

    @Column(name = "admin_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decisao", nullable = false)
    private Decisao decisao;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @PrePersist
    void prePersist() {
        this.criadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public UUID getAdminId() { return adminId; }
    public void setAdminId(UUID adminId) { this.adminId = adminId; }
    public Decisao getDecisao() { return decisao; }
    public void setDecisao(Decisao decisao) { this.decisao = decisao; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Instant getCriadoEm() { return criadoEm; }
}
