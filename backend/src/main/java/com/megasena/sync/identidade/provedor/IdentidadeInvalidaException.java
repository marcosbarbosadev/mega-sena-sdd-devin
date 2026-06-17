package com.megasena.sync.identidade.provedor;

public class IdentidadeInvalidaException extends RuntimeException {
    public IdentidadeInvalidaException(String message) {
        super(message);
    }

    public IdentidadeInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
