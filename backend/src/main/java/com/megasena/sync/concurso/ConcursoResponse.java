package com.megasena.sync.concurso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ConcursoResponse(
        int numero,
        LocalDate dataSorteio,
        List<Integer> dezenas,
        BigDecimal valorPremio
) {
    public static ConcursoResponse from(Concurso concurso) {
        List<Integer> dezenas = concurso.getDezenas().stream()
                .map(ConcursoDezena::getDezena)
                .sorted()
                .toList();
        return new ConcursoResponse(
                concurso.getNumero(),
                concurso.getDataSorteio(),
                dezenas,
                concurso.getValorPremio()
        );
    }
}
