CREATE TABLE sync_run (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    origem VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    iniciado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalizado_em TIMESTAMP NULL,
    concurso_inicial INT NULL,
    concurso_final INT NULL,
    concursos_importados INT NOT NULL DEFAULT 0,
    tentativas INT NOT NULL DEFAULT 1,
    mensagem_erro TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sync_run_status ON sync_run(status);
CREATE INDEX idx_sync_run_iniciado ON sync_run(iniciado_em DESC);
