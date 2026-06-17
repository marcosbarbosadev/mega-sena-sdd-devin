package com.megasena.sync.admin;

import com.megasena.sync.sincronizacao.SyncRun;

import java.time.LocalDateTime;

public record SyncRunResponse(
        Long id,
        String origem,
        String status,
        LocalDateTime iniciadoEm,
        LocalDateTime finalizadoEm,
        Integer concursoInicial,
        Integer concursoFinal,
        int concursosImportados,
        int tentativas,
        String mensagemErro
) {
    public static SyncRunResponse from(SyncRun run) {
        return new SyncRunResponse(
                run.getId(),
                run.getOrigem().name(),
                run.getStatus().name(),
                run.getIniciadoEm(),
                run.getFinalizadoEm(),
                run.getConcursoInicial(),
                run.getConcursoFinal(),
                run.getConcursosImportados(),
                run.getTentativas(),
                run.getMensagemErro()
        );
    }
}
