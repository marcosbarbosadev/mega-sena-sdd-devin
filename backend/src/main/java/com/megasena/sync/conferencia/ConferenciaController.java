package com.megasena.sync.conferencia;

import com.megasena.sync.config.UsuarioAutenticado;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ConferenciaController {

    private final ConferenciaService conferenciaService;

    public ConferenciaController(ConferenciaService conferenciaService) {
        this.conferenciaService = conferenciaService;
    }

    @GetMapping("/jogos/{id}/conferencia")
    public ResponseEntity<ConferenciaResponse> conferir(@PathVariable UUID id,
                                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(conferenciaService.conferir(id, usuario.getUsuarioId()));
    }

    @GetMapping("/conferencias")
    public ResponseEntity<List<ConferenciaResponse>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(conferenciaService.listarConferencias(usuario.getUsuarioId()));
    }
}
