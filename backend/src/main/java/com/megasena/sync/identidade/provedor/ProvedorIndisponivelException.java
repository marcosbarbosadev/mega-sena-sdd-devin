package com.megasena.sync.identidade.provedor;

public class ProvedorIndisponivelException extends RuntimeException {
    public ProvedorIndisponivelException(String message) {
        super(message);
    }

    public ProvedorIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
