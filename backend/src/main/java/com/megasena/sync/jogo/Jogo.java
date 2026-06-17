package com.megasena.sync.jogo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jogo")
public class Jogo {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "usuario_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID usuarioId;

    @Column(name = "concurso_numero")
    private Integer concursoNumero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_selecao", nullable = false)
    private TipoSelecao tipoSelecao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @OneToMany(mappedBy = "jogo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<JogoDezena> dezenas = new ArrayList<>();

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

    public void addDezena(int dezena) {
        JogoDezena jd = new JogoDezena();
        jd.setJogo(this);
        jd.setDezena(dezena);
        this.dezenas.add(jd);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public Integer getConcursoNumero() { return concursoNumero; }
    public void setConcursoNumero(Integer concursoNumero) { this.concursoNumero = concursoNumero; }
    public TipoSelecao getTipoSelecao() { return tipoSelecao; }
    public void setTipoSelecao(TipoSelecao tipoSelecao) { this.tipoSelecao = tipoSelecao; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public List<JogoDezena> getDezenas() { return dezenas; }
    public void setDezenas(List<JogoDezena> dezenas) { this.dezenas = dezenas; }
}
