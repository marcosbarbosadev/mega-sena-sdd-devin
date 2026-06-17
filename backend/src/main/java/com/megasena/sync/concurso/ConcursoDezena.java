package com.megasena.sync.concurso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "concurso_dezena", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"concurso_numero", "dezena"})
})
public class ConcursoDezena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concurso_numero", nullable = false)
    private Concurso concurso;

    @Column(name = "dezena", nullable = false)
    private Integer dezena;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Concurso getConcurso() { return concurso; }
    public void setConcurso(Concurso concurso) { this.concurso = concurso; }
    public Integer getDezena() { return dezena; }
    public void setDezena(Integer dezena) { this.dezena = dezena; }
}
