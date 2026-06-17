package com.megasena.sync.sincronizacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_run")
public class SyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    private OrigemSync origem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusSync status;

    @Column(name = "iniciado_em", nullable = false)
    private LocalDateTime iniciadoEm;

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @Column(name = "concurso_inicial")
    private Integer concursoInicial;

    @Column(name = "concurso_final")
    private Integer concursoFinal;

    @Column(name = "concursos_importados", nullable = false)
    private int concursosImportados;

    @Column(name = "tentativas", nullable = false)
    private int tentativas = 1;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    public static SyncRun iniciar(OrigemSync origem) {
        SyncRun run = new SyncRun();
        run.setOrigem(origem);
        run.setStatus(StatusSync.EM_EXECUCAO);
        run.setIniciadoEm(LocalDateTime.now());
        run.setConcursosImportados(0);
        run.setTentativas(1);
        return run;
    }

    public void concluirSucesso(int concursoInicial, int concursoFinal, int importados) {
        this.status = StatusSync.SUCESSO;
        this.finalizadoEm = LocalDateTime.now();
        this.concursoInicial = concursoInicial;
        this.concursoFinal = concursoFinal;
        this.concursosImportados = importados;
    }

    public void concluirFalha(String mensagemErro) {
        this.status = StatusSync.FALHA;
        this.finalizadoEm = LocalDateTime.now();
        this.mensagemErro = mensagemErro;
    }

    public void concluirParcial(int concursoInicial, int concursoFinal, int importados, String mensagemErro) {
        this.status = StatusSync.PARCIAL;
        this.finalizadoEm = LocalDateTime.now();
        this.concursoInicial = concursoInicial;
        this.concursoFinal = concursoFinal;
        this.concursosImportados = importados;
        this.mensagemErro = mensagemErro;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrigemSync getOrigem() { return origem; }
    public void setOrigem(OrigemSync origem) { this.origem = origem; }
    public StatusSync getStatus() { return status; }
    public void setStatus(StatusSync status) { this.status = status; }
    public LocalDateTime getIniciadoEm() { return iniciadoEm; }
    public void setIniciadoEm(LocalDateTime iniciadoEm) { this.iniciadoEm = iniciadoEm; }
    public LocalDateTime getFinalizadoEm() { return finalizadoEm; }
    public void setFinalizadoEm(LocalDateTime finalizadoEm) { this.finalizadoEm = finalizadoEm; }
    public Integer getConcursoInicial() { return concursoInicial; }
    public void setConcursoInicial(Integer concursoInicial) { this.concursoInicial = concursoInicial; }
    public Integer getConcursoFinal() { return concursoFinal; }
    public void setConcursoFinal(Integer concursoFinal) { this.concursoFinal = concursoFinal; }
    public int getConcursosImportados() { return concursosImportados; }
    public void setConcursosImportados(int concursosImportados) { this.concursosImportados = concursosImportados; }
    public int getTentativas() { return tentativas; }
    public void setTentativas(int tentativas) { this.tentativas = tentativas; }
    public String getMensagemErro() { return mensagemErro; }
    public void setMensagemErro(String mensagemErro) { this.mensagemErro = mensagemErro; }
}
