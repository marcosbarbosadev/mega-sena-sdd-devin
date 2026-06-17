CREATE TABLE evento_jogo (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    jogo_id         BINARY(16)      NOT NULL,
    usuario_id      BINARY(16)      NOT NULL,
    tipo            ENUM('CADASTRO','EDICAO','EXCLUSAO') NOT NULL,
    criado_em       DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_evento_jogo (jogo_id),
    KEY idx_evento_jogo_usuario (usuario_id),
    CONSTRAINT fk_evento_jogo_jogo FOREIGN KEY (jogo_id) REFERENCES jogo(id),
    CONSTRAINT fk_evento_jogo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
