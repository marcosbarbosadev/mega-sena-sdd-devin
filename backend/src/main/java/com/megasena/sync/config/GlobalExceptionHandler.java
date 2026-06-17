package com.megasena.sync.config;

import com.megasena.sync.identidade.EmailNaoVerificadoException;
import com.megasena.sync.identidade.provedor.ProvedorIndisponivelException;
import com.megasena.sync.moderacao.ContaNaoEncontradaException;
import com.megasena.sync.moderacao.TransicaoInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(404, "NAO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(409, "CONFLITO", ex.getMessage()));
    }

    @ExceptionHandler(EmailNaoVerificadoException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNaoVerificado(EmailNaoVerificadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(403, "EMAIL_NAO_VERIFICADO", ex.getMessage()));
    }

    @ExceptionHandler(ProvedorIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleProvedorIndisponivel(ProvedorIndisponivelException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody(503, "PROVEDOR_INDISPONIVEL", "Serviço de identidade temporariamente indisponível."));
    }

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleContaNaoEncontrada(ContaNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(404, "NAO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleTransicaoInvalida(TransicaoInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(409, "TRANSICAO_INVALIDA", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Dados inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(400, "VALIDACAO", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, "ERRO_INTERNO", "Erro interno do servidor"));
    }

    private Map<String, Object> errorBody(int status, String codigo, String mensagem) {
        return Map.of("status", status, "codigo", codigo, "mensagem", mensagem);
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }
}
