CREATE TABLE evento_conferencia (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    jogo_id         BINARY(16)      NOT NULL,
    concurso_numero INT             NOT NULL,
    usuario_id      BINARY(16)      NOT NULL,
    acertos         INT             NOT NULL,
    faixa           ENUM('SENA','QUINA','QUADRA','NENHUMA') NOT NULL,
    premiado        BOOLEAN         NOT NULL,
    criado_em       DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conferencia_jogo_concurso (jogo_id, concurso_numero),
    KEY idx_conferencia_usuario (usuario_id),
    CONSTRAINT fk_conferencia_jogo FOREIGN KEY (jogo_id) REFERENCES jogo(id),
    CONSTRAINT fk_conferencia_concurso FOREIGN KEY (concurso_numero) REFERENCES concurso(numero),
    CONSTRAINT fk_conferencia_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
