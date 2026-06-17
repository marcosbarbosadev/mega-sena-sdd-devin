package com.megasena.sync.identidade.provedor;

public interface VerificadorDeIdentidade {

    IdentidadeVerificada verify(String idToken);

    void revogarSessoes(String uid);
}
