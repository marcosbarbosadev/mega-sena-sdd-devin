package com.megasena.sync.identidade;

import com.megasena.sync.config.UsuarioAutenticado;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final UsuarioRepository usuarioRepository;

    public PerfilController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<ContaResponse> perfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioRepository.findById(usuario.getUsuarioId())
                .map(u -> ResponseEntity.ok(ContaResponse.from(u)))
                .orElse(ResponseEntity.notFound().build());
    }
}
