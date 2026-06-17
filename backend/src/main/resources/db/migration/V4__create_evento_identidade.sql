CREATE TABLE evento_identidade (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    usuario_id      BINARY(16)      NULL,
    tipo            ENUM('CADASTRO','AUTENTICACAO','LOGOUT','ACESSO_NEGADO') NOT NULL,
    metodo_login    ENUM('SENHA','GOOGLE') NULL,
    sucesso         BOOLEAN         NOT NULL,
    motivo          VARCHAR(80)     NULL,
    correlation_id  VARCHAR(64)     NULL,
    criado_em       DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_evento_usuario (usuario_id),
    KEY idx_evento_tipo_data (tipo, criado_em),
    CONSTRAINT fk_evento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
