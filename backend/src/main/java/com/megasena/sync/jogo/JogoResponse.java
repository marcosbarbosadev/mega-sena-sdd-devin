package com.megasena.sync.jogo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JogoResponse(
        UUID id,
        Integer concursoNumero,
        TipoSelecao tipoSelecao,
        List<Integer> dezenas,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static JogoResponse from(Jogo jogo) {
        List<Integer> dezenas = jogo.getDezenas().stream()
                .map(JogoDezena::getDezena)
                .sorted()
                .toList();
        return new JogoResponse(
                jogo.getId(),
                jogo.getConcursoNumero(),
                jogo.getTipoSelecao(),
                dezenas,
                jogo.getCriadoEm(),
                jogo.getAtualizadoEm()
        );
    }
}
