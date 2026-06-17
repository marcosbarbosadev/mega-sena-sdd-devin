CREATE TABLE concurso (
    numero INT NOT NULL PRIMARY KEY,
    data_sorteio DATE NOT NULL,
    valor_premio DECIMAL(15,2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE concurso_dezena (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    concurso_numero INT NOT NULL,
    dezena INT NOT NULL,
    CONSTRAINT fk_concurso_dezena_concurso
        FOREIGN KEY (concurso_numero) REFERENCES concurso(numero)
        ON DELETE CASCADE,
    CONSTRAINT uk_concurso_dezena
        UNIQUE (concurso_numero, dezena),
    CONSTRAINT chk_dezena_range CHECK (dezena BETWEEN 1 AND 60)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_concurso_data_sorteio ON concurso(data_sorteio);
CREATE INDEX idx_concurso_dezena_dezena ON concurso_dezena(dezena);
