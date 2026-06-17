CREATE TABLE usuario (
    id              BINARY(16)      NOT NULL,
    provider_uid    VARCHAR(128)    NOT NULL,
    email           VARCHAR(320)    NOT NULL,
    papel           ENUM('USUARIO','ADMINISTRADOR') NOT NULL DEFAULT 'USUARIO',
    estado          ENUM('PENDENTE','ATIVO','REPROVADO') NOT NULL DEFAULT 'PENDENTE',
    metodo_login    ENUM('SENHA','GOOGLE') NOT NULL,
    criado_em       DATETIME(6)     NOT NULL,
    atualizado_em   DATETIME(6)     NOT NULL,
    ultimo_acesso_em DATETIME(6)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_provider_uid (provider_uid),
    UNIQUE KEY uk_usuario_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
