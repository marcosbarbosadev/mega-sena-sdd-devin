package com.megasena.sync.concurso;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/concursos")
public class ConcursoController {

    private final ConcursoService concursoService;

    public ConcursoController(ConcursoService concursoService) {
        this.concursoService = concursoService;
    }

    @GetMapping("/latest")
    public ResponseEntity<ConcursoResponse> getLatest() {
        Concurso concurso = concursoService.buscarUltimo();
        return ResponseEntity.ok(ConcursoResponse.from(concurso));
    }

    @GetMapping("/{numero}")
    public ResponseEntity<ConcursoResponse> getByNumero(@PathVariable int numero) {
        Concurso concurso = concursoService.buscarPorNumero(numero);
        return ResponseEntity.ok(ConcursoResponse.from(concurso));
    }
}
