package com.megasena.sync.moderacao;

import jakarta.validation.constraints.NotBlank;

public record ReprovacaoRequest(
        @NotBlank(message = "Motivo é obrigatório na reprovação")
        String motivo
) {}
