package com.megasena.sync.moderacao;

import com.megasena.sync.config.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/contas")
public class ModeracaoController {

    private final ModeracaoService moderacaoService;

    public ModeracaoController(ModeracaoService moderacaoService) {
        this.moderacaoService = moderacaoService;
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<ContaPendenteResponse>> listarPendentes() {
        return ResponseEntity.ok(moderacaoService.listarPendentes());
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<Void> aprovar(@PathVariable UUID id,
                                        @AuthenticationPrincipal UsuarioAutenticado admin) {
        moderacaoService.aprovar(id, admin.getUsuarioId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reprovar")
    public ResponseEntity<Void> reprovar(@PathVariable UUID id,
                                         @Valid @RequestBody ReprovacaoRequest request,
                                         @AuthenticationPrincipal UsuarioAutenticado admin) {
        moderacaoService.reprovar(id, admin.getUsuarioId(), request.motivo());
        return ResponseEntity.noContent().build();
    }
}
