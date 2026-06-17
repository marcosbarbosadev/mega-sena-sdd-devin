package com.megasena.sync.jogo;

import java.util.List;

public record JogoRequest(
        List<Integer> dezenas,
        Integer quantidade,
        Integer concursoNumero
) {}
