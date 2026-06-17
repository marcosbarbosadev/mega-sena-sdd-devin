package com.megasena.sync.support;

import com.megasena.sync.identidade.MetodoLogin;
import com.megasena.sync.identidade.provedor.IdentidadeInvalidaException;
import com.megasena.sync.identidade.provedor.IdentidadeVerificada;
import com.megasena.sync.identidade.provedor.ProvedorIndisponivelException;
import com.megasena.sync.identidade.provedor.VerificadorDeIdentidade;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class VerificadorDeIdentidadeFake implements VerificadorDeIdentidade {

    private final Map<String, IdentidadeVerificada> tokens = new ConcurrentHashMap<>();
    private final List<String> sessoesRevogadas = new ArrayList<>();
    private boolean simularIndisponibilidade = false;

    public void registrarToken(String token, String uid, String email, boolean emailVerificado, MetodoLogin metodo) {
        tokens.put(token, new IdentidadeVerificada(uid, email, emailVerificado, metodo));
    }

    public void registrarToken(String token, IdentidadeVerificada identidade) {
        tokens.put(token, identidade);
    }

    public void simularIndisponibilidade(boolean ativo) {
        this.simularIndisponibilidade = ativo;
    }

    public List<String> getSessoesRevogadas() {
        return List.copyOf(sessoesRevogadas);
    }

    public void limpar() {
        tokens.clear();
        sessoesRevogadas.clear();
        simularIndisponibilidade = false;
    }

    @Override
    public IdentidadeVerificada verify(String idToken) {
        if (simularIndisponibilidade) {
            throw new ProvedorIndisponivelException("Provedor simulado como indisponível");
        }
        IdentidadeVerificada identidade = tokens.get(idToken);
        if (identidade == null) {
            throw new IdentidadeInvalidaException("Token inválido (fake)");
        }
        return identidade;
    }

    @Override
    public void revogarSessoes(String uid) {
        if (simularIndisponibilidade) {
            throw new ProvedorIndisponivelException("Provedor simulado como indisponível");
        }
        sessoesRevogadas.add(uid);
    }
}
