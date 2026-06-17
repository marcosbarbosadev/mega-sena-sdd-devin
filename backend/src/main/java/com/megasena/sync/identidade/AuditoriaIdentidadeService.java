package com.megasena.sync.identidade;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditoriaIdentidadeService {

    private final EventoIdentidadeRepository repository;

    public AuditoriaIdentidadeService(EventoIdentidadeRepository repository) {
        this.repository = repository;
    }

    public void registrar(UUID usuarioId, TipoEvento tipo, MetodoLogin metodoLogin, boolean sucesso, String motivo) {
        EventoIdentidade evento = new EventoIdentidade();
        evento.setUsuarioId(usuarioId);
        evento.setTipo(tipo);
        evento.setMetodoLogin(metodoLogin);
        evento.setSucesso(sucesso);
        evento.setMotivo(motivo);
        repository.save(evento);
    }
}
