package com.megasena.sync.identidade;

import com.megasena.sync.config.UsuarioAutenticado;
import com.megasena.sync.identidade.provedor.VerificadorDeIdentidade;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class ContaController {

    private final UsuarioRepository usuarioRepository;
    private final VerificadorDeIdentidade verificador;
    private final AuditoriaIdentidadeService auditoria;

    public ContaController(UsuarioRepository usuarioRepository,
                           VerificadorDeIdentidade verificador,
                           AuditoriaIdentidadeService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.verificador = verificador;
        this.auditoria = auditoria;
    }

    @GetMapping("/me")
    public ResponseEntity<ContaResponse> me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioRepository.findById(usuario.getUsuarioId())
                .map(u -> ResponseEntity.ok(ContaResponse.from(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        verificador.revogarSessoes(usuario.getProviderUid());
        auditoria.registrar(usuario.getUsuarioId(), TipoEvento.LOGOUT, null, true, null);
        return ResponseEntity.noContent().build();
    }
}
