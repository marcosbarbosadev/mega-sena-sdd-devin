CREATE TABLE jogo (
    id              BINARY(16)      NOT NULL,
    usuario_id      BINARY(16)      NOT NULL,
    concurso_numero INT             NULL,
    tipo_selecao    ENUM('MANUAL','AUTOMATICO') NOT NULL,
    criado_em       DATETIME(6)     NOT NULL,
    atualizado_em   DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_jogo_usuario (usuario_id),
    KEY idx_jogo_concurso (concurso_numero),
    CONSTRAINT fk_jogo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_jogo_concurso FOREIGN KEY (concurso_numero) REFERENCES concurso(numero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE jogo_dezena (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    jogo_id         BINARY(16)      NOT NULL,
    dezena          INT             NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jogo_dezena (jogo_id, dezena),
    CONSTRAINT fk_jogo_dezena_jogo FOREIGN KEY (jogo_id) REFERENCES jogo(id) ON DELETE CASCADE,
    CONSTRAINT ck_dezena_range CHECK (dezena >= 1 AND dezena <= 60)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
