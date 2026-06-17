package com.megasena.sync.identidade;

import java.time.Instant;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        String email,
        Papel papel,
        EstadoConta estado,
        MetodoLogin metodoLogin,
        Instant ultimoAcessoEm
) {
    public static ContaResponse from(Usuario usuario) {
        return new ContaResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.getEstado(),
                usuario.getMetodoLogin(),
                usuario.getUltimoAcessoEm()
        );
    }
}
