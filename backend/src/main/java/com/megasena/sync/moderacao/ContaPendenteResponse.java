package com.megasena.sync.moderacao;

import com.megasena.sync.identidade.MetodoLogin;
import com.megasena.sync.identidade.Usuario;

import java.time.Instant;
import java.util.UUID;

public record ContaPendenteResponse(
        UUID id,
        String email,
        MetodoLogin metodoLogin,
        Instant criadoEm
) {
    public static ContaPendenteResponse from(Usuario usuario) {
        return new ContaPendenteResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getMetodoLogin(),
                usuario.getCriadoEm()
        );
    }
}
