package com.megasena.sync.jogo;

import com.megasena.sync.config.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jogos")
public class JogoController {

    private final JogoService jogoService;

    public JogoController(JogoService jogoService) {
        this.jogoService = jogoService;
    }

    @PostMapping
    public ResponseEntity<JogoResponse> criar(@RequestBody JogoRequest request,
                                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jogoService.criar(request, usuario.getUsuarioId()));
    }

    @GetMapping
    public ResponseEntity<List<JogoResponse>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(jogoService.listarPorUsuario(usuario.getUsuarioId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JogoResponse> editar(@PathVariable UUID id,
                                               @RequestBody JogoRequest request,
                                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(jogoService.editar(id, request, usuario.getUsuarioId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        jogoService.excluir(id, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
