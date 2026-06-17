package com.megasena.sync.concurso;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "concurso")
public class Concurso {

    @Id
    @Column(name = "numero")
    private Integer numero;

    @Column(name = "data_sorteio", nullable = false)
    private LocalDate dataSorteio;

    @Column(name = "valor_premio", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPremio;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "concurso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ConcursoDezena> dezenas = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
    }

    @PreUpdate
    void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public LocalDate getDataSorteio() { return dataSorteio; }
    public void setDataSorteio(LocalDate dataSorteio) { this.dataSorteio = dataSorteio; }
    public BigDecimal getValorPremio() { return valorPremio; }
    public void setValorPremio(BigDecimal valorPremio) { this.valorPremio = valorPremio; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public List<ConcursoDezena> getDezenas() { return dezenas; }
    public void setDezenas(List<ConcursoDezena> dezenas) { this.dezenas = dezenas; }

    public void addDezena(int dezena) {
        ConcursoDezena cd = new ConcursoDezena();
        cd.setConcurso(this);
        cd.setDezena(dezena);
        this.dezenas.add(cd);
    }
}
