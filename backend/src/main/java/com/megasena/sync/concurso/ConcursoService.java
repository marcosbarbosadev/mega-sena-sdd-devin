package com.megasena.sync.concurso;

import com.megasena.sync.config.GlobalExceptionHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConcursoService {

    private final ConcursoRepository concursoRepository;

    public ConcursoService(ConcursoRepository concursoRepository) {
        this.concursoRepository = concursoRepository;
    }

    public Concurso buscarUltimo() {
        return concursoRepository.findTopByOrderByNumeroDesc()
                .orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException(
                        "Nenhum concurso sincronizado ainda"));
    }

    public Concurso buscarPorNumero(int numero) {
        return concursoRepository.findByNumero(numero)
                .orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException(
                        "Concurso " + numero + " não encontrado"));
    }
}
