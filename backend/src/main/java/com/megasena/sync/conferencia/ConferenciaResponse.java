package com.megasena.sync.conferencia;

import java.util.List;
import java.util.UUID;

public record ConferenciaResponse(
        UUID jogoId,
        Integer concursoNumero,
        List<Integer> dezenasJogadas,
        List<Integer> dezenasSorteadas,
        int acertos,
        Faixa faixa,
        boolean premiado,
        String status
) {}
