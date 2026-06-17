package com.megasena.sync.identidade;

import com.megasena.sync.config.UsuarioAutenticado;
import com.megasena.sync.identidade.provedor.IdentidadeVerificada;

public interface ResolvedorDeConta {
    UsuarioAutenticado resolver(IdentidadeVerificada identidade);
}
