package com.megasena.sync.identidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "provider_uid", nullable = false, length = 128, unique = true)
    private String providerUid;

    @Column(name = "email", nullable = false, length = 320, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false)
    private Papel papel = Papel.USUARIO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoConta estado = EstadoConta.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_login", nullable = false)
    private MetodoLogin metodoLogin;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "ultimo_acesso_em")
    private Instant ultimoAcessoEm;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
    }

    @PreUpdate
    void preUpdate() {
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getProviderUid() { return providerUid; }
    public void setProviderUid(String providerUid) { this.providerUid = providerUid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Papel getPapel() { return papel; }
    public void setPapel(Papel papel) { this.papel = papel; }
    public EstadoConta getEstado() { return estado; }
    public void setEstado(EstadoConta estado) { this.estado = estado; }
    public MetodoLogin getMetodoLogin() { return metodoLogin; }
    public void setMetodoLogin(MetodoLogin metodoLogin) { this.metodoLogin = metodoLogin; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public Instant getUltimoAcessoEm() { return ultimoAcessoEm; }
    public void setUltimoAcessoEm(Instant ultimoAcessoEm) { this.ultimoAcessoEm = ultimoAcessoEm; }
}
