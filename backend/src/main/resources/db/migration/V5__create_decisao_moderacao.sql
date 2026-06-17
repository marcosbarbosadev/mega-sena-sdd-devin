CREATE TABLE decisao_moderacao (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    usuario_id      BINARY(16)      NOT NULL,
    admin_id        BINARY(16)      NOT NULL,
    decisao         ENUM('APROVADO','REPROVADO') NOT NULL,
    motivo          VARCHAR(500)    NULL,
    criado_em       DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_decisao_usuario (usuario_id),
    KEY idx_decisao_admin_data (admin_id, criado_em),
    CONSTRAINT fk_decisao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_decisao_admin FOREIGN KEY (admin_id) REFERENCES usuario(id),
    CONSTRAINT ck_decisao_motivo CHECK ((decisao='REPROVADO' AND motivo IS NOT NULL) OR (decisao='APROVADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
