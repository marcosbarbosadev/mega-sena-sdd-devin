package com.megasena.sync.identidade.provedor;

import com.megasena.sync.identidade.MetodoLogin;

public record IdentidadeVerificada(
        String uid,
        String email,
        boolean emailVerificado,
        MetodoLogin metodoLogin
) {}
