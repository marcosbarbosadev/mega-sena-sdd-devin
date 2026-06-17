package com.megasena.sync.sincronizacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {

    Optional<SyncRun> findTopByOrderByIniciadoEmDesc();

    boolean existsByStatus(StatusSync status);
}
